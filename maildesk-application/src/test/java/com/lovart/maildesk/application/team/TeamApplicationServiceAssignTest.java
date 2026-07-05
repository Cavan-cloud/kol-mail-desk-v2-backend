package com.lovart.maildesk.application.team;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServiceAssignTest {

    @Mock
    private KolMapper kols;

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
        service = new TeamApplicationService(profiles, kols, credentials, new AuditLogService(actions));
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
}
