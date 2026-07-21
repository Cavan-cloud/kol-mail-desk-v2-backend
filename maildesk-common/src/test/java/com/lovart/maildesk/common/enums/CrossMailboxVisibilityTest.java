package com.lovart.maildesk.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrossMailboxVisibilityTest {

    @Test
    void fromConfig_parsesAliases() {
        assertThat(CrossMailboxVisibility.fromConfig("non_intern")).isEqualTo(CrossMailboxVisibility.NON_INTERN);
        assertThat(CrossMailboxVisibility.fromConfig("leader-only")).isEqualTo(CrossMailboxVisibility.LEADER_ONLY);
        assertThat(CrossMailboxVisibility.fromConfig("OWN_ONLY")).isEqualTo(CrossMailboxVisibility.OWN_ONLY);
        assertThat(CrossMailboxVisibility.fromConfig(null)).isEqualTo(CrossMailboxVisibility.NON_INTERN);
    }

    @Test
    void fromConfig_rejectsUnknown() {
        assertThatThrownBy(() -> CrossMailboxVisibility.fromConfig("everyone"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
