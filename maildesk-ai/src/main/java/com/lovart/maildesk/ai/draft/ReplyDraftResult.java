package com.lovart.maildesk.ai.draft;

/**
 * Bilingual reply draft output ({@code english} send-ready, {@code chinese} for operator review).
 */
public record ReplyDraftResult(String english, String chinese, boolean fallback, String aiError) {}
