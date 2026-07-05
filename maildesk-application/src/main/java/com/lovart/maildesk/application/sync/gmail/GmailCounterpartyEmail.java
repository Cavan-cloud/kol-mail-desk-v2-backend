package com.lovart.maildesk.application.sync.gmail;

import com.lovart.maildesk.common.enums.EmailDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class GmailCounterpartyEmail {

    private GmailCounterpartyEmail() {
    }

    static String resolve(
            EmailDirection direction,
            String fromEmail,
            List<String> toEmails,
            List<String> ccEmails,
            String ownEmail) {
        String own = normalize(ownEmail);
        String from = normalize(fromEmail);
        List<String> recipients = new ArrayList<>(toEmails == null ? List.of() : toEmails);
        if (ccEmails != null) {
            recipients.addAll(ccEmails);
        }
        String firstExternal = recipients.stream()
                .map(GmailCounterpartyEmail::normalize)
                .filter(email -> !email.isBlank() && !email.equals(own))
                .findFirst()
                .orElse(null);
        if (direction == EmailDirection.INBOUND) {
            if (!from.equals(own)) {
                return from;
            }
            return firstExternal != null ? firstExternal : from;
        }
        return firstExternal != null ? firstExternal : recipients.stream().findFirst().orElse(from);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
