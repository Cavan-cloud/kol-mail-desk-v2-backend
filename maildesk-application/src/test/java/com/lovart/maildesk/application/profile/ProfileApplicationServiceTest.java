package com.lovart.maildesk.application.profile;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.TeamProfileUpdateRequest;
import com.lovart.maildesk.application.team.TeamApplicationService;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileApplicationServiceTest {

    @Mock
    private ProfileMapper profiles;

    @Mock
    private IntegrationCredentialMapper credentials;

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private ActionMapper actions;

    private ProfileApplicationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        TeamApplicationService teamService =
                new TeamApplicationService(profiles, kols, emails, credentials, new AuditLogService(actions));
        service = new ProfileApplicationService(
                profiles, credentials, teamService, new AuditLogService(actions));
        userId = UUID.randomUUID();
    }

    @Test
    void updateOwnProfile_assignsKolsWhenFeishuNameSaved() {
        ProfileDO profile = activeProfile("full_time");
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.selectCount(any())).thenReturn(0L);
        when(kols.update(any(), any(UpdateWrapper.class))).thenReturn(3);

        var result = service.updateOwnProfile(
                userId,
                new TeamProfileUpdateRequest("Chloe", UserRole.FULL_TIME, null, "王雨"));

        assertThat(result.kolsAssigned()).isEqualTo(3);
        verify(profiles).updateById(profile);
        assertThat(profile.getFeishuOperatorName()).isEqualTo("王雨");
    }

    @Test
    void updateOwnProfile_rejectsLeaderPromotionForNonLeader() {
        ProfileDO profile = activeProfile("full_time");
        when(profiles.selectById(userId)).thenReturn(profile);

        assertThatThrownBy(() -> service.updateOwnProfile(
                        userId,
                        new TeamProfileUpdateRequest("Chloe", UserRole.LEADER, null, "王雨")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void updateOwnProfile_allowsLeaderSelectionDuringPendingApproval() {
        ProfileDO profile = activeProfile("full_time");
        profile.setStatus(UserStatus.PENDING_APPROVAL.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);
        when(credentials.selectCount(any())).thenReturn(0L);

        var result = service.updateOwnProfile(
                userId,
                new TeamProfileUpdateRequest("Chloe", UserRole.LEADER, null, "王雨"));

        assertThat(result.profile().role()).isEqualTo("leader");
        verify(actions).insert(any(ActionDO.class));
    }

    @Test
    void updateOwnProfile_rejectsMissingFeishuOperatorName() {
        ProfileDO profile = activeProfile("full_time");
        profile.setStatus(UserStatus.PENDING_APPROVAL.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        assertThatThrownBy(() -> service.updateOwnProfile(
                        userId,
                        new TeamProfileUpdateRequest("Chloe", UserRole.FULL_TIME, null, "  ")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getMessage())
                .isEqualTo("请填写飞书运营名");
    }

    @Test
    void updateOwnProfile_rejectsDepartedAccount() {
        ProfileDO profile = activeProfile("full_time");
        profile.setStatus(UserStatus.DEPARTED.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        assertThatThrownBy(() -> service.updateOwnProfile(
                        userId,
                        new TeamProfileUpdateRequest("Chloe", UserRole.FULL_TIME, null, "王雨")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void updateOwnProfile_rejectsInternWithoutMentor() {
        ProfileDO profile = activeProfile("full_time");
        profile.setStatus(UserStatus.PENDING_APPROVAL.dbValue());
        when(profiles.selectById(userId)).thenReturn(profile);

        assertThatThrownBy(() -> service.updateOwnProfile(
                        userId,
                        new TeamProfileUpdateRequest("Chloe", UserRole.INTERN, null, "王雨")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getMessage())
                .isEqualTo("实习生必须选择 mentor");
    }

    private ProfileDO activeProfile(String role) {
        ProfileDO profile = new ProfileDO();
        profile.setId(userId);
        profile.setDisplayName("Chloe");
        profile.setRole(role);
        profile.setStatus(UserStatus.ACTIVE.dbValue());
        return profile;
    }
}
