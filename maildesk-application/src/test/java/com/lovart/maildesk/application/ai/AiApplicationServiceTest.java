package com.lovart.maildesk.application.ai;

import com.lovart.maildesk.ai.AiService;
import com.lovart.maildesk.ai.check.CheckDraftRequest;
import com.lovart.maildesk.ai.check.CheckDraftResult;
import com.lovart.maildesk.ai.draft.ReplyDraftRequest;
import com.lovart.maildesk.ai.draft.ReplyDraftResult;
import com.lovart.maildesk.ai.translate.TranslateTextRequest;
import com.lovart.maildesk.ai.translate.TranslateTextResult;
import com.lovart.maildesk.application.dto.AiCheckRequest;
import com.lovart.maildesk.application.dto.AiDraftRequest;
import com.lovart.maildesk.application.dto.AiTranslateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiApplicationServiceTest {

    private AiApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AiApplicationService(new StubAiService());
    }

    @Test
    void generateDraft_mapsEnglishAndChineseFields() {
        var result = service.generateDraft(new AiDraftRequest(
                "Alice",
                null,
                "Latest email body",
                "",
                null,
                "outreach",
                "YouTube",
                100.0));

        assertThat(result.englishDraft()).isEqualTo("Hello");
        assertThat(result.chineseDraft()).isEqualTo("你好");
    }

    @Test
    void checkDraft_wrapsIssuesForApi() {
        var result = service.checkDraft(new AiCheckRequest("draft text long enough", null, "Alice"));

        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().getFirst().message()).isEqualTo("缺少报价");
    }

    @Test
    void translate_usesSendDraftModeForEnglish() {
        var result = service.translate(new AiTranslateRequest("你好", "en"));

        assertThat(result.translated()).isEqualTo("Hello");
        assertThat(result.targetLang()).isEqualTo("en");
    }

    private static final class StubAiService extends AiService {
        StubAiService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public ReplyDraftResult generateReplyDraft(ReplyDraftRequest request) {
            return new ReplyDraftResult("Hello", "你好", false, null);
        }

        @Override
        public CheckDraftResult checkDraft(CheckDraftRequest request) {
            return new CheckDraftResult(List.of("缺少报价"), false, null);
        }

        @Override
        public TranslateTextResult translateText(TranslateTextRequest request) {
            return new TranslateTextResult("Hello", request.targetLang(), false, null);
        }
    }
}
