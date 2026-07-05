package com.lovart.maildesk.application.dto;

public record AiTranslateResult(String translated, String targetLang, boolean fallback) {
}
