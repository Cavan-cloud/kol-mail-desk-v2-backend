package com.lovart.maildesk.application.dto;

import java.util.List;

public record AiCheckResult(List<AiCheckIssue> issues, boolean fallback) {

    public record AiCheckIssue(String severity, String message) {
    }
}
