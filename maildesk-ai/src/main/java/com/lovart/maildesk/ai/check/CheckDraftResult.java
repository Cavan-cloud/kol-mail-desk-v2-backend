package com.lovart.maildesk.ai.check;

import java.util.List;

/**
 * Pre-send draft review output ({@code issues} empty when no problems found).
 */
public record CheckDraftResult(List<String> issues, boolean fallback, String aiError) {}
