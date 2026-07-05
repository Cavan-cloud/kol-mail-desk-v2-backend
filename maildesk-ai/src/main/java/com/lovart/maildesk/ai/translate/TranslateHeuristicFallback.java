package com.lovart.maildesk.ai.translate;

/**
 * Fallback when no AI provider is configured or all providers fail.
 * Per cost design: surface a user-visible hint instead of throwing.
 */
public final class TranslateHeuristicFallback {

    private TranslateHeuristicFallback() {}

    public static TranslateTextResult notConfigured(TranslateTextRequest request) {
        String message =
                request.targetLang() == TranslateTargetLang.ZH
                        ? "AI 翻译未配置，请配置 MOONSHOT_API_KEY 或 DEEPSEEK_API_KEY。"
                        : "AI translation is not configured. Please set MOONSHOT_API_KEY or DEEPSEEK_API_KEY.";
        return new TranslateTextResult(message, request.targetLang(), true, null);
    }

    public static TranslateTextResult failed(TranslateTextRequest request) {
        return failed(request, "AI 翻译失败");
    }

    public static TranslateTextResult failed(TranslateTextRequest request, String aiError) {
        String message =
                request.targetLang() == TranslateTargetLang.ZH
                        ? "AI 翻译失败，请稍后重试。"
                        : "AI translation failed. Please retry later.";
        return new TranslateTextResult(message, request.targetLang(), true, aiError);
    }
}
