package com.lovart.maildesk.application.support;

import com.lovart.maildesk.domain.email.entity.EmailDO;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDedupeTest {

    @Test
    void prefersViewerCopyForSameGmailMessageId() {
        UUID viewer = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        OffsetDateTime sent = OffsetDateTime.now(ZoneOffset.UTC);

        EmailDO otherCopy = email("msg-1", other, sent, false);
        EmailDO viewerCopy = email("msg-1", viewer, sent, true);

        List<EmailDO> result = EmailDedupe.dedupeForViewer(List.of(otherCopy, viewerCopy), viewer);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(viewer);
        assertThat(result.getFirst().getIsRead()).isTrue();
    }

    @Test
    void keepsDistinctMessagesOrderedBySentAtDesc() {
        UUID viewer = UUID.randomUUID();
        OffsetDateTime older = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime newer = OffsetDateTime.parse("2026-02-01T00:00:00Z");

        EmailDO a = email("msg-a", viewer, older, true);
        EmailDO b = email("msg-b", viewer, newer, true);

        List<EmailDO> result = EmailDedupe.dedupeForViewer(List.of(a, b), viewer);

        assertThat(result).extracting(EmailDO::getGmailMessageId).containsExactly("msg-b", "msg-a");
    }

    private static EmailDO email(String messageId, UUID userId, OffsetDateTime sentAt, boolean read) {
        EmailDO row = new EmailDO();
        row.setId(UUID.randomUUID());
        row.setGmailMessageId(messageId);
        row.setUserId(userId);
        row.setSentAt(sentAt);
        row.setIsRead(read);
        return row;
    }
}
