package com.lovart.maildesk.domain.gmail;

import java.util.List;

public record GmailMessageListPage(List<String> messageIds, String nextPageToken, String mode) {
}
