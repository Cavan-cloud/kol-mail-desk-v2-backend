package com.lovart.maildesk.application.sync.gmail;

import com.lovart.maildesk.common.enums.EmailDirection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GmailCounterpartyEmailTest {

    @Test
    void inbound_usesFromWhenExternal() {
        String email = GmailCounterpartyEmail.resolve(
                EmailDirection.INBOUND,
                "kol@example.com",
                List.of("me@company.com"),
                List.of(),
                "me@company.com");
        assertThat(email).isEqualTo("kol@example.com");
    }

    @Test
    void outbound_usesFirstExternalRecipient() {
        String email = GmailCounterpartyEmail.resolve(
                EmailDirection.OUTBOUND,
                "me@company.com",
                List.of("kol@example.com", "other@example.com"),
                List.of(),
                "me@company.com");
        assertThat(email).isEqualTo("kol@example.com");
    }
}
