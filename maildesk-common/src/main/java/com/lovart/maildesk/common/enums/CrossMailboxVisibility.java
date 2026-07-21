package com.lovart.maildesk.common.enums;

/**
 * Workbench policy for reading emails synced under other users' Gmail accounts.
 *
 * <ul>
 *   <li>{@link #OWN_ONLY} — current user sees only {@code emails.user_id = me}</li>
 *   <li>{@link #LEADER_ONLY} — leaders may read all mailboxes; others stay own-only</li>
 *   <li>{@link #NON_INTERN} — everyone except interns may read all mailboxes</li>
 * </ul>
 */
public enum CrossMailboxVisibility {
    OWN_ONLY,
    LEADER_ONLY,
    NON_INTERN;

    public static CrossMailboxVisibility fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return NON_INTERN;
        }
        String normalized = raw.trim().toLowerCase().replace('-', '_');
        return switch (normalized) {
            case "own_only", "ownonly" -> OWN_ONLY;
            case "leader_only", "leaderonly" -> LEADER_ONLY;
            case "non_intern", "nonintern" -> NON_INTERN;
            default -> throw new IllegalArgumentException(
                    "Unknown maildesk.workbench.cross-mailbox-visibility: " + raw);
        };
    }
}
