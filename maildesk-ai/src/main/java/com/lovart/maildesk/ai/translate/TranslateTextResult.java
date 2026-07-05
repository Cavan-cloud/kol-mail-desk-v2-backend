package com.lovart.maildesk.ai.translate;

/**
 * On-demand translation output (plain text, no JSON envelope from the LLM).
 */
public record TranslateTextResult(
        String translated, TranslateTargetLang targetLang, boolean fallback, String aiError) {}
