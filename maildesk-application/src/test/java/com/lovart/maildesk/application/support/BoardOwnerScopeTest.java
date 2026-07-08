package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardOwnerScopeTest {

    @Test
    void resolveScopeOwnerIds_nullOwner_returnsNullForTeamWide() {
        assertThat(BoardOwnerScope.resolveScopeOwnerIds(null, true, List.of())).isNull();
    }

    @Test
    void resolveScopeOwnerIds_includesInternsUnderMentor() {
        UUID mentorId = UUID.randomUUID();
        UUID internId = UUID.randomUUID();
        ProfileDO mentor = profile(mentorId, "leader", null);
        ProfileDO intern = profile(internId, "intern", mentorId);

        Set<UUID> scope = BoardOwnerScope.resolveScopeOwnerIds(mentorId, true, List.of(mentor, intern));

        assertThat(scope).containsExactlyInAnyOrder(mentorId, internId);
    }

    @Test
    void resolveScopeOwnerIds_excludesInternsWhenDisabled() {
        UUID mentorId = UUID.randomUUID();
        UUID internId = UUID.randomUUID();
        ProfileDO mentor = profile(mentorId, "leader", null);
        ProfileDO intern = profile(internId, "intern", mentorId);

        Set<UUID> scope = BoardOwnerScope.resolveScopeOwnerIds(mentorId, false, List.of(mentor, intern));

        assertThat(scope).containsExactly(mentorId);
    }

    @Test
    void resolveScopeOwnerIds_unknownOwner_throwsNotFound() {
        assertThatThrownBy(() -> BoardOwnerScope.resolveScopeOwnerIds(UUID.randomUUID(), true, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("成员不存在");
    }

    private static ProfileDO profile(UUID id, String role, UUID mentorUserId) {
        ProfileDO profile = new ProfileDO();
        profile.setId(id);
        profile.setRole(role);
        profile.setStatus("active");
        profile.setMentorUserId(mentorUserId);
        return profile;
    }
}
