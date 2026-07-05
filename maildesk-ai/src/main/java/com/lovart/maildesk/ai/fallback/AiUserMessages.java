package com.lovart.maildesk.ai.fallback;

/**
 * User-visible AI fallback copy (Simplified Chinese unless noted).
 */
public final class AiUserMessages {

    public static final String CLASSIFY_HEURISTIC_SUMMARY = "待人工复核";
    public static final String CLASSIFY_HEURISTIC_ACTION = "检查并回复";

    public static final String CLASSIFY_FAILURE_SUMMARY = "AI 分类失败，已保留邮件等待人工处理。";
    public static final String CLASSIFY_FAILURE_ACTION = "请人工查看邮件内容并补充分类。";
    public static final String CLASSIFY_AI_ERROR = "AI 分类失败";

    public static final String DRAFT_AI_ERROR = "AI 草稿生成失败";
    public static final String CHECK_AI_ERROR = "AI 草稿检查失败";
    public static final String TRANSLATE_AI_ERROR = "AI 翻译失败";

    private AiUserMessages() {}
}
