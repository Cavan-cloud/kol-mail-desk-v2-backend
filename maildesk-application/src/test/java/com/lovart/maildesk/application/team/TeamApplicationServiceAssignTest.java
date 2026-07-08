package com.lovart.maildesk.application.team;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServiceAssignTest {

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private com.lovart.maildesk.domain.profile.mapper.ProfileMapper profiles;

    @Mock
    private com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper credentials;

    @Mock
    private ActionMapper actions;

    private TeamApplicationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new TeamApplicationService(profiles, kols, emails, credentials, new AuditLogService(actions));
        userId = UUID.randomUUID();
    }

    @Test
    void assignKolsByOperatorName_updatesOnlyUnownedRows() {
        when(kols.update(any(), any(UpdateWrapper.class))).thenReturn(3);

        int assigned = service.assignKolsByOperatorName(userId, "@张瑞 ");

        assertThat(assigned).isEqualTo(3);
        ArgumentCaptor<UpdateWrapper<KolDO>> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(kols).update(any(), captor.capture());
    }

    @Test
    void assignKolsByOperatorName_skipsBlankName() {
        assertThat(service.assignKolsByOperatorName(userId, "  ")).isZero();
        assertThat(service.assignKolsByOperatorName(userId, null)).isZero();
    }

    @Test
    void reconcileAndAssignKolsByOperatorName_releasesGmailDuplicatesBeforeClaim() {
        UUID gmailDuplicateId = UUID.randomUUID();
        UUID feishuTargetId = UUID.randomUUID();

        KolDO gmailDuplicate = new KolDO();
        gmailDuplicate.setId(gmailDuplicateId);
        gmailDuplicate.setEmail("creator@example.com");
        gmailDuplicate.setSource("gmail");
        gmailDuplicate.setOwnerUserId(userId);
        gmailDuplicate.setFeishuOperatorName("");

        KolDO feishuTarget = new KolDO();
        feishuTarget.setId(feishuTargetId);
        feishuTarget.setEmail("creator@example.com");
        feishuTarget.setSource("feishu");
        feishuTarget.setFeishuRecordId("rec-1");
        feishuTarget.setFeishuOperatorName("潘慧妍");

        when(kols.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(gmailDuplicate))
                .thenReturn(List.of(feishuTarget));
        when(kols.update(any(), any(UpdateWrapper.class))).thenReturn(2);

        TeamApplicationService.OperatorClaimResult result =
                service.reconcileAndAssignKolsByOperatorName(userId, "潘慧妍");

        assertThat(result.released()).isEqualTo(1);
        assertThat(result.assigned()).isEqualTo(2);
        verify(kols).updateById(any(KolDO.class));
        verify(emails).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void reconcileAndAssignKolsByOperatorName_keepsCorrectlyOwnedFeishuRows() {
        KolDO owned = new KolDO();
        owned.setId(UUID.randomUUID());
        owned.setSource("feishu");
        owned.setFeishuOperatorName("潘慧妍");
        owned.setOwnerUserId(userId);

        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(owned));
        when(kols.update(any(), any(UpdateWrapper.class))).thenReturn(0);

        TeamApplicationService.OperatorClaimResult result =
                service.reconcileAndAssignKolsByOperatorName(userId, "潘慧妍");

        assertThat(result.released()).isZero();
        verify(kols, never()).updateById(any(KolDO.class));
    }
}
