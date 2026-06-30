package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.kol.entity.KolDO;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Workbench priority signals ported from legacy {@code lib/workbench.ts}.
 */
public final class WorkbenchRules {

    private static final Duration UNREPLIED_THRESHOLD = Duration.ofDays(3);

    private WorkbenchRules() {
    }

    /**
     * Stalled-thread heuristic for the team pool: last inbound is 3+ days unanswered.
     */
    public static boolean isUnreplied(KolDO kol, OffsetDateTime now) {
        if (kol == null || kol.getLastInboundAt() == null) {
            return false;
        }
        OffsetDateTime lastInbound = kol.getLastInboundAt();
        OffsetDateTime lastOutbound = kol.getLastOutboundAt();
        if (lastOutbound != null && lastOutbound.isAfter(lastInbound)) {
            return false;
        }
        return Duration.between(lastInbound, now).compareTo(UNREPLIED_THRESHOLD) >= 0;
    }

    /**
     * Needs my reply: latest email is inbound and not manually resolved.
     */
    public static boolean needsMyReply(EmailDO latestEmail, boolean replyResolved) {
        if (replyResolved) {
            return false;
        }
        return latestEmail != null && latestEmail.getDirection() == EmailDirection.INBOUND;
    }

    /**
     * Awaiting their reply: latest email is outbound.
     */
    public static boolean awaitingTheirReply(EmailDO latestEmail) {
        return latestEmail != null && latestEmail.getDirection() == EmailDirection.OUTBOUND;
    }

    /**
     * High priority inbound: latest email is inbound with AI priority {@code high}.
     */
    public static boolean isHighPriorityInbound(EmailDO latestEmail) {
        return latestEmail != null
                && latestEmail.getDirection() == EmailDirection.INBOUND
                && "high".equals(latestEmail.getAiPriority());
    }
}
