package com.lovart.maildesk.domain.gmail;

import java.util.List;

public record GmailHistoryPage(List<String> messageIds, String mode) {
}
