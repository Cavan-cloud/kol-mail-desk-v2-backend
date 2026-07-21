package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.enums.CrossMailboxVisibility;
import com.lovart.maildesk.common.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVisibilityPolicyTest {

    private final EmailVisibilityPolicy policy = new EmailVisibilityPolicy();

    @Test
    void ownOnly_neverAllowsCrossMailbox() {
        assertThat(policy.canViewCrossMailbox(UserRole.LEADER, CrossMailboxVisibility.OWN_ONLY)).isFalse();
        assertThat(policy.canViewCrossMailbox(UserRole.FULL_TIME, CrossMailboxVisibility.OWN_ONLY)).isFalse();
        assertThat(policy.canViewCrossMailbox(UserRole.INTERN, CrossMailboxVisibility.OWN_ONLY)).isFalse();
    }

    @Test
    void leaderOnly_allowsLeaderOnly() {
        assertThat(policy.canViewCrossMailbox(UserRole.LEADER, CrossMailboxVisibility.LEADER_ONLY)).isTrue();
        assertThat(policy.canViewCrossMailbox(UserRole.FULL_TIME, CrossMailboxVisibility.LEADER_ONLY)).isFalse();
        assertThat(policy.canViewCrossMailbox(UserRole.MEMBER, CrossMailboxVisibility.LEADER_ONLY)).isFalse();
        assertThat(policy.canViewCrossMailbox(UserRole.INTERN, CrossMailboxVisibility.LEADER_ONLY)).isFalse();
    }

    @Test
    void nonIntern_blocksOnlyIntern() {
        assertThat(policy.canViewCrossMailbox(UserRole.LEADER, CrossMailboxVisibility.NON_INTERN)).isTrue();
        assertThat(policy.canViewCrossMailbox(UserRole.FULL_TIME, CrossMailboxVisibility.NON_INTERN)).isTrue();
        assertThat(policy.canViewCrossMailbox(UserRole.MEMBER, CrossMailboxVisibility.NON_INTERN)).isTrue();
        assertThat(policy.canViewCrossMailbox(UserRole.INTERN, CrossMailboxVisibility.NON_INTERN)).isFalse();
    }
}
