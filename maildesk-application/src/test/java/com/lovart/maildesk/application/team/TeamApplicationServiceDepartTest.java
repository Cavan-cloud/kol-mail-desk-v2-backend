package com.lovart.maildesk.application.team;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.DepartTeamMemberResult;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServiceDepartTest {

    @Mock
    private ProfileMapper profiles;

    @Mock
    private KolMapper kols;

    @Mock
    private com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper credentials;

    @Mock
    private ActionMapper actions;

    private TeamApplicationService service;
    private UUID leaderId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        service = new TeamApplicationService(profiles, kols, credentials, new AuditLogService(actions));
        leaderId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    void departMember_orphansActiveKolsAndMarksProfileDeparted() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));
        when(profiles.selectById(targetId)).thenReturn(activeProfile(targetId, UserRole.MEMBER));
        when(profiles.updateById(any(ProfileDO.class))).thenReturn(1);
        when(kols.update(any(), any(UpdateWrapper.class))).thenReturn(2);

        DepartTeamMemberResult result = service.departMember(leaderId, targetId);

        assertThat(result.orphanedCount()).isEqualTo(2);
        verify(actions).insert(any(ActionDO.class));
    }

    @Test
    void departMember_rejectsNonLeader() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.MEMBER));

        assertThatThrownBy(() -> service.departMember(leaderId, targetId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void departMember_rejectsSelfDepart() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));

        assertThatThrownBy(() -> service.departMember(leaderId, leaderId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能将自己标记离职");
    }

    @Test
    void departMember_rejectsAlreadyDepartedTarget() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));
        ProfileDO departed = activeProfile(targetId, UserRole.MEMBER);
        departed.setStatus(UserStatus.DEPARTED.dbValue());
        when(profiles.selectById(targetId)).thenReturn(departed);

        assertThatThrownBy(() -> service.departMember(leaderId, targetId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("CONFLICT");
    }

    private static ProfileDO activeProfile(UUID id, UserRole role) {
        ProfileDO profile = new ProfileDO();
        profile.setId(id);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        profile.setRole(role.dbValue());
        return profile;
    }
}
