package com.lovart.maildesk.ai.translate;

/**
 * Fallback when no AI provider is configured or all providers fail.
 * Per cost design: surface a user-visible hint instead of throwing.
 */
public final class TranslateHeuristicFallback {

    private TranslateHeuristicFallback() {}

    public static TranslateTextResult notConfigured(TranslateTextRequest request) {
        String message =
                switch (request.targetLang()) {
                    case ZH -> "AI 翻译未配置，请配置 MOONSHOT_API_KEY 或 DEEPSEEK_API_KEY。";
                    case EN -> "AI translation is not configured. Please set MOONSHOT_API_KEY or DEEPSEEK_API_KEY.";
                    case KO -> "AI 번역이 구성되지 않았습니다. MOONSHOT_API_KEY 또는 DEEPSEEK_API_KEY를 설정하세요.";
                };
        return new TranslateTextResult(message, request.targetLang(), true, null);
    }

    public static TranslateTextResult failed(TranslateTextRequest request) {
        return failed(request, "AI 翻译失败");
    }

    public static TranslateTextResult failed(TranslateTextRequest request, String aiError) {
        String message =
                switch (request.targetLang()) {
                    case ZH -> "AI 翻译失败，请稍后重试。";
                    case EN -> "AI translation failed. Please retry later.";
                    case KO -> "AI 번역에 실패했습니다. 잠시 후 다시 시도하세요.";
                };
        return new TranslateTextResult(message, request.targetLang(), true, aiError);
    }
}
