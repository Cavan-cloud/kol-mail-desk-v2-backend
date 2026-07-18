package com.lovart.maildesk.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.ai.check.CheckDraftHeuristicFallback;
import com.lovart.maildesk.ai.check.CheckDraftParser;
import com.lovart.maildesk.ai.check.CheckDraftRequest;
import com.lovart.maildesk.ai.check.CheckDraftResult;
import com.lovart.maildesk.ai.classify.ClassifyEmailHeuristicFallback;
import com.lovart.maildesk.ai.classify.ClassifyEmailRequest;
import com.lovart.maildesk.ai.classify.EmailClassificationParser;
import com.lovart.maildesk.ai.classify.EmailClassificationResult;
import com.lovart.maildesk.ai.draft.ReplyDraftHeuristicFallback;
import com.lovart.maildesk.ai.draft.ReplyDraftParser;
import com.lovart.maildesk.ai.draft.ReplyDraftRequest;
import com.lovart.maildesk.ai.draft.ReplyDraftResult;
import com.lovart.maildesk.ai.fallback.AiFailureClassifier;
import com.lovart.maildesk.ai.fallback.AiFailureReason;
import com.lovart.maildesk.ai.fallback.AiInvocationFallbacks;
import com.lovart.maildesk.ai.fallback.AiInvocationPipeline;
import com.lovart.maildesk.ai.fallback.AiUserMessages;
import com.lovart.maildesk.ai.prompt.AiPromptCatalog;
import com.lovart.maildesk.ai.prompt.AiPromptKey;
import com.lovart.maildesk.ai.translate.TranslateHeuristicFallback;
import com.lovart.maildesk.ai.translate.TranslateModelSelector;
import com.lovart.maildesk.ai.translate.TranslateTargetLang;
import com.lovart.maildesk.ai.translate.TranslateTextRequest;
import com.lovart.maildesk.ai.translate.TranslateTextResult;
import com.lovart.maildesk.ai.usage.AiCallMetrics;
import com.lovart.maildesk.ai.usage.AiInvocationAttempt;
import com.lovart.maildesk.common.enums.KolStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import com.lovart.maildesk.ai.config.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI entry points for classify / draft / check / translate (Phase 4).
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiInvocationPipeline invocationPipeline;
    private final AiPromptCatalog promptCatalog;
    private final EmailClassificationParser classificationParser;
    private final ReplyDraftParser replyDraftParser;
    private final CheckDraftParser checkDraftParser;
    private final ObjectMapper objectMapper;

    public AiService(
            AiInvocationPipeline invocationPipeline,
            AiPromptCatalog promptCatalog,
            EmailClassificationParser classificationParser,
            ReplyDraftParser replyDraftParser,
            CheckDraftParser checkDraftParser,
            ObjectMapper objectMapper) {
        this.invocationPipeline = invocationPipeline;
        this.promptCatalog = promptCatalog;
        this.classificationParser = classificationParser;
        this.replyDraftParser = replyDraftParser;
        this.checkDraftParser = checkDraftParser;
        this.objectMapper = objectMapper;
    }

    /**
     * Classify a single email (summary, priority, stage_signal hint, extracted fields).
     * Uses configured LLM providers with primary → fallback → heuristic degradation.
     */
    public EmailClassificationResult classifyEmail(ClassifyEmailRequest request) {
        return invocationPipeline.run(
                AiCapability.CLASSIFY,
                "classifyEmail",
                target -> invokeClassify(target, request),
                new AiInvocationFallbacks<>(
                        () -> ClassifyEmailHeuristicFallback.classify(request),
                        () -> ClassifyEmailHeuristicFallback.classify(request, AiUserMessages.CLASSIFY_AI_ERROR)));
    }

    /**
     * Generate bilingual reply draft (English send-ready + Chinese operator copy).
     */
    public ReplyDraftResult generateReplyDraft(ReplyDraftRequest request) {
        return invocationPipeline.run(
                AiCapability.DRAFT,
                "generateReplyDraft",
                target -> invokeReplyDraft(target, request),
                new AiInvocationFallbacks<>(
                        () -> ReplyDraftHeuristicFallback.draft(request),
                        () -> ReplyDraftHeuristicFallback.draft(request, AiUserMessages.DRAFT_AI_ERROR)));
    }

    /**
     * Review draft for missing brief / deadline / deliverable before send.
     */
    public CheckDraftResult checkDraft(CheckDraftRequest request) {
        return invocationPipeline.run(
                AiCapability.CHECK,
                "checkDraft",
                target -> invokeCheckDraft(target, request),
                new AiInvocationFallbacks<>(
                        CheckDraftHeuristicFallback::empty,
                        () -> CheckDraftHeuristicFallback.empty(AiUserMessages.CHECK_AI_ERROR)));
    }

    /**
     * On-demand bilingual translation (email body or send draft).
     */
    public TranslateTextResult translateText(TranslateTextRequest request) {
        return invocationPipeline.run(
                AiCapability.TRANSLATE,
                "translateText",
                target -> invokeTranslate(target, request),
                new AiInvocationFallbacks<>(
                        () -> TranslateHeuristicFallback.notConfigured(request),
                        () -> TranslateHeuristicFallback.failed(request, AiUserMessages.TRANSLATE_AI_ERROR)));
    }

    private AiInvocationAttempt<EmailClassificationResult> invokeClassify(AiResolvedTarget target, ClassifyEmailRequest request) {
        long started = System.currentTimeMillis();
        try {
            String systemPrompt = promptCatalog.systemPrompt(AiPromptKey.CLASSIFY_EMAIL);
            String userPayload = buildClassifyUserPayload(request);
            OpenAiChatOptions options = DeepSeekChatOptions.builder(target)
                    .temperature(0.1)
                    .responseFormat(ResponseFormat.builder()
                            .type(ResponseFormat.Type.JSON_OBJECT)
                            .build())
                    .build();
            Prompt prompt = new Prompt(
                    java.util.List.of(new SystemMessage(systemPrompt), new UserMessage(userPayload)), options);
            ChatResponse response = target.chatModel().call(prompt);
            String content = response.getResult().getOutput().getText();
            EmailClassificationResult parsed = classificationParser.parse(content);
            log.debug(
                    "[ai] classifyEmail succeeded provider={} model={} fallback={}",
                    target.providerId(),
                    target.model(),
                    target.fallback());
            return AiInvocationAttempt.success(
                    parsed,
                    metrics(target, true, started, response));
        } catch (RuntimeException ex) {
            logProviderFailure("classifyEmail", target, ex);
            return AiInvocationAttempt.failure(failureMetrics(target, started));
        }
    }

    private AiInvocationAttempt<ReplyDraftResult> invokeReplyDraft(AiResolvedTarget target, ReplyDraftRequest request) {
        long started = System.currentTimeMillis();
        try {
            String systemPrompt = promptCatalog.systemPrompt(AiPromptKey.REPLY_DRAFT);
            String userPayload = buildReplyDraftUserPayload(request);
            OpenAiChatOptions options = DeepSeekChatOptions.builder(target)
                    .temperature(0.3)
                    .responseFormat(ResponseFormat.builder()
                            .type(ResponseFormat.Type.JSON_OBJECT)
                            .build())
                    .build();
            Prompt prompt = new Prompt(
                    java.util.List.of(new SystemMessage(systemPrompt), new UserMessage(userPayload)), options);
            ChatResponse response = target.chatModel().call(prompt);
            String content = response.getResult().getOutput().getText();
            ReplyDraftResult parsed = replyDraftParser.parse(content);
            log.debug(
                    "[ai] generateReplyDraft succeeded provider={} model={} fallback={}",
                    target.providerId(),
                    target.model(),
                    target.fallback());
            return AiInvocationAttempt.success(parsed, metrics(target, true, started, response));
        } catch (RuntimeException ex) {
            logProviderFailure("generateReplyDraft", target, ex);
            return AiInvocationAttempt.failure(failureMetrics(target, started));
        }
    }

    private AiInvocationAttempt<CheckDraftResult> invokeCheckDraft(AiResolvedTarget target, CheckDraftRequest request) {
        long started = System.currentTimeMillis();
        try {
            String systemPrompt = promptCatalog.systemPrompt(AiPromptKey.CHECK_DRAFT);
            String userPayload = buildCheckDraftUserPayload(request);
            OpenAiChatOptions options = DeepSeekChatOptions.builder(target)
                    .temperature(0.1)
                    .responseFormat(ResponseFormat.builder()
                            .type(ResponseFormat.Type.JSON_OBJECT)
                            .build())
                    .build();
            Prompt prompt = new Prompt(
                    java.util.List.of(new SystemMessage(systemPrompt), new UserMessage(userPayload)), options);
            ChatResponse response = target.chatModel().call(prompt);
            String content = response.getResult().getOutput().getText();
            CheckDraftResult parsed = checkDraftParser.parse(content);
            log.debug(
                    "[ai] checkDraft succeeded provider={} model={} fallback={}",
                    target.providerId(),
                    target.model(),
                    target.fallback());
            return AiInvocationAttempt.success(parsed, metrics(target, true, started, response));
        } catch (RuntimeException ex) {
            logProviderFailure("checkDraft", target, ex);
            return AiInvocationAttempt.failure(metrics(target, false, started, null));
        }
    }

    private AiInvocationAttempt<TranslateTextResult> invokeTranslate(AiResolvedTarget target, TranslateTextRequest request) {
        long started = System.currentTimeMillis();
        try {
            AiPromptKey promptKey = switch (request.targetLang()) {
                case EN -> AiPromptKey.TRANSLATE_ZH_TO_EN;
                case KO -> AiPromptKey.TRANSLATE_ZH_TO_KO;
                case ZH -> AiPromptKey.TRANSLATE_EMAIL_TO_ZH;
            };
            String systemPrompt = promptCatalog.systemPrompt(promptKey);
            String userPayload = buildTranslateUserPayload(request);
            String model = TranslateModelSelector.selectModel(target.model(), request.text().length());
            OpenAiChatOptions options = DeepSeekChatOptions.builder(target)
                    .model(model)
                    .temperature(0.2)
                    .build();
            Prompt prompt = new Prompt(
                    java.util.List.of(new SystemMessage(systemPrompt), new UserMessage(userPayload)), options);
            ChatResponse response = target.chatModel().call(prompt);
            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("AI returned empty translation");
            }
            log.debug(
                    "[ai] translateText succeeded provider={} model={} fallback={} target={}",
                    target.providerId(),
                    model,
                    target.fallback(),
                    request.targetLang().apiValue());
            AiCallMetrics callMetrics = AiCallMetrics.of(
                    target.providerId(),
                    model,
                    true,
                    elapsedMs(started),
                    promptTokens(response),
                    completionTokens(response),
                    target.fallback());
            return AiInvocationAttempt.success(
                    new TranslateTextResult(content.trim(), request.targetLang(), false, null), callMetrics);
        } catch (RuntimeException ex) {
            logProviderFailure("translateText", target, ex);
            return AiInvocationAttempt.failure(failureMetrics(target, started));
        }
    }

    private static AiCallMetrics metrics(
            AiResolvedTarget target, boolean success, long startedMs, ChatResponse response) {
        return AiCallMetrics.of(
                target,
                success,
                elapsedMs(startedMs),
                promptTokens(response),
                completionTokens(response));
    }

    private static AiCallMetrics failureMetrics(AiResolvedTarget target, long startedMs) {
        return AiCallMetrics.of(target, false, elapsedMs(startedMs), null, null);
    }

    private static int elapsedMs(long startedMs) {
        return (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startedMs);
    }

    private static Integer promptTokens(ChatResponse response) {
        Usage usage = usageOf(response);
        return usage == null ? null : usage.getPromptTokens();
    }

    private static Integer completionTokens(ChatResponse response) {
        Usage usage = usageOf(response);
        return usage == null ? null : usage.getCompletionTokens();
    }

    private static Usage usageOf(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return null;
        }
        return response.getMetadata().getUsage();
    }

    private static void logProviderFailure(String operation, AiResolvedTarget target, RuntimeException ex) {
        AiFailureReason reason = AiFailureClassifier.classify(ex);
        log.warn(
                "[ai] {} failed provider={} model={} reason={}: {}",
                operation,
                target.providerId(),
                target.model(),
                reason,
                ex.getMessage());
    }

    private String buildClassifyUserPayload(ClassifyEmailRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("direction", request.direction().name().toLowerCase(Locale.ROOT));
        payload.put("subject", request.subject() == null ? "" : request.subject());
        payload.put("body", request.body());
        payload.put("history", request.history() == null ? "" : request.history());
        return writeJson(payload);
    }

    private String buildReplyDraftUserPayload(ReplyDraftRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kol_name", request.kolName());
        payload.put("sender_name", request.senderName());
        payload.put("latest_email", request.latestEmail());
        payload.put("history", request.history() == null ? "" : request.history());
        payload.put("template_hint", request.templateHint() == null ? "" : request.templateHint());
        payload.put("kol_stage", formatStage(request.kolStage()));
        payload.put("kol_platform", request.kolPlatform() == null ? "" : request.kolPlatform());
        payload.put("kol_agreed_price", request.kolAgreedPrice());
        return writeJson(payload);
    }

    private String buildCheckDraftUserPayload(CheckDraftRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("draft", request.draft());
        payload.put("kol_stage", formatStage(request.kolStage()));
        payload.put("kol_platform", request.kolPlatform() == null ? "" : request.kolPlatform());
        payload.put("kol_name", request.kolName() == null ? "" : request.kolName());
        return writeJson(payload);
    }

    private String buildTranslateUserPayload(TranslateTextRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", request.mode().apiValue());
        payload.put("target", request.targetLang().apiValue());
        payload.put("text", request.text());
        return writeJson(payload);
    }

    private static String formatStage(KolStage stage) {
        if (stage == null) {
            return "";
        }
        return stage.name().toLowerCase(Locale.ROOT);
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize AI payload", ex);
        }
    }
}
