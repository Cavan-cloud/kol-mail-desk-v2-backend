package com.lovart.maildesk.application.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.KolAssignRequest;
import com.lovart.maildesk.application.dto.KolAssignResult;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServicePoolAssignTest {

    @Mock
    private ProfileMapper profiles;

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper credentials;

    @Mock
    private ActionMapper actions;

    private TeamApplicationService service;
    private UUID leaderId;
    private UUID assigneeId;
    private UUID kolId;

    @BeforeEach
    void setUp() {
        service = new TeamApplicationService(profiles, kols, emails, credentials, new AuditLogService(actions));
        leaderId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        kolId = UUID.randomUUID();
    }

    @Test
    void assignPoolKols_assignsOrphanedKolsToActiveMember() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));
        when(profiles.selectById(assigneeId)).thenReturn(activeProfile(assigneeId, UserRole.MEMBER));

        KolDO orphaned = new KolDO();
        orphaned.setId(kolId);
        orphaned.setStatus(KolStatus.ORPHANED);
        orphaned.setOwnerUserId(UUID.randomUUID());
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(orphaned));
        when(kols.updateById(any(KolDO.class))).thenReturn(1);

        KolAssignResult result = service.assignPoolKols(
                leaderId,
                new KolAssignRequest(List.of(kolId), assigneeId));

        assertThat(result.assignedCount()).isEqualTo(1);
        verify(actions).insert(any(ActionDO.class));
    }

    @Test
    void assignPoolKols_assignsUnassignedKolsToActiveMember() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));
        when(profiles.selectById(assigneeId)).thenReturn(activeProfile(assigneeId, UserRole.MEMBER));

        KolDO unassigned = new KolDO();
        unassigned.setId(kolId);
        unassigned.setStatus(KolStatus.UNASSIGNED);
        unassigned.setOwnerUserId(null);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(unassigned));
        when(kols.updateById(any(KolDO.class))).thenReturn(1);

        KolAssignResult result = service.assignPoolKols(
                leaderId,
                new KolAssignRequest(List.of(kolId), assigneeId));

        assertThat(result.assignedCount()).isEqualTo(1);
        verify(actions).insert(any(ActionDO.class));
    }

    @Test
    void assignPoolKols_rejectsNonPoolKols() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));
        when(profiles.selectById(assigneeId)).thenReturn(activeProfile(assigneeId, UserRole.MEMBER));

        KolDO active = new KolDO();
        active.setId(kolId);
        active.setStatus(KolStatus.ACTIVE);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(active));

        assertThatThrownBy(() -> service.assignPoolKols(
                        leaderId,
                        new KolAssignRequest(List.of(kolId), assigneeId)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("CONFLICT");
    }

    @Test
    void assignPoolKols_rejectsDepartedAssignee() {
        when(profiles.selectById(leaderId)).thenReturn(activeProfile(leaderId, UserRole.LEADER));
        ProfileDO departed = activeProfile(assigneeId, UserRole.MEMBER);
        departed.setStatus(UserStatus.DEPARTED.dbValue());
        when(profiles.selectById(assigneeId)).thenReturn(departed);

        assertThatThrownBy(() -> service.assignPoolKols(
                        leaderId,
                        new KolAssignRequest(List.of(kolId), assigneeId)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("VALIDATION_ERROR");
    }

    private static ProfileDO activeProfile(UUID id, UserRole role) {
        ProfileDO profile = new ProfileDO();
        profile.setId(id);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        profile.setRole(role.dbValue());
        return profile;
    }
}
