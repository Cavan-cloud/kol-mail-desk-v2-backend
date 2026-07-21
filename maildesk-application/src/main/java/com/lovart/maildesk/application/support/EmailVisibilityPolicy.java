package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.enums.CrossMailboxVisibility;
import com.lovart.maildesk.common.enums.UserRole;
import org.springframework.stereotype.Component;

/**
 * Decides whether the current user may read emails whose {@code user_id} is not themselves.
 * Write paths (mark read / reclassify) stay own-mailbox regardless of this policy.
 */
@Component
public class EmailVisibilityPolicy {

    public boolean canViewCrossMailbox(UserRole role, CrossMailboxVisibility mode) {
        CrossMailboxVisibility effective = mode == null ? CrossMailboxVisibility.NON_INTERN : mode;
        UserRole effectiveRole = role == null ? UserRole.MEMBER : role;
        return switch (effective) {
            case OWN_ONLY -> false;
            case LEADER_ONLY -> effectiveRole == UserRole.LEADER;
            case NON_INTERN -> effectiveRole != UserRole.INTERN;
        };
    }
}
