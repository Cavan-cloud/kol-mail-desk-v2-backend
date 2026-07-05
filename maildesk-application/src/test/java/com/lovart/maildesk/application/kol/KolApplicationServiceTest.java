package com.lovart.maildesk.application.kol;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.KolUpdateRequest;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.audit.entity.ActionDO;
import com.lovart.maildesk.domain.audit.mapper.ActionMapper;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KolApplicationServiceTest {

    @Mock
    private KolMapper kols;

    @Mock
    private ActionMapper actions;

    private AuditLogService auditLog;
    private KolApplicationService service;
    private UUID userId;
    private UUID kolId;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLogService(actions);
        service = new KolApplicationService(kols, auditLog);
        userId = UUID.randomUUID();
        kolId = UUID.randomUUID();
    }

    @Test
    void updateKol_renamesOwnedKolAndMarksNameOverridden() {
        KolDO existing = ownedKol("Alice");
        when(kols.selectById(kolId)).thenReturn(existing);

        var result = service.updateKol(userId, kolId, new KolUpdateRequest("  Bob  ", null, null));

        assertThat(result.name()).isEqualTo("Bob");

        ArgumentCaptor<KolDO> captor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(captor.capture());
        KolDO patch = captor.getValue();
        assertThat(patch.getId()).isEqualTo(kolId);
        assertThat(patch.getName()).isEqualTo("Bob");
        assertThat(patch.getNameOverridden()).isTrue();

        ArgumentCaptor<ActionDO> auditCaptor = ArgumentCaptor.forClass(ActionDO.class);
        verify(actions).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getMetadata().get("field").asText()).isEqualTo("name");
    }

    @Test
    void updateKol_setsStageOverrideAndAuditsStageChange() {
        KolDO existing = ownedKol("Alice");
        existing.setStage(KolStage.OUTREACH);
        when(kols.selectById(kolId)).thenReturn(existing);

        var result = service.updateKol(userId, kolId, new KolUpdateRequest(null, KolStage.NEGOTIATING, null));

        assertThat(result.stage()).isEqualTo("negotiating");
        assertThat(result.stageOverride()).isTrue();

        ArgumentCaptor<KolDO> captor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(captor.capture());
        assertThat(captor.getValue().getStage()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(captor.getValue().getStageOverride()).isTrue();
    }

    @Test
    void updateKol_setsReplyResolvedAndAudits() {
        KolDO existing = ownedKol("Alice");
        when(kols.selectById(kolId)).thenReturn(existing);

        var result = service.updateKol(userId, kolId, new KolUpdateRequest(null, null, true));

        assertThat(result.replyResolved()).isTrue();

        ArgumentCaptor<KolDO> captor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(captor.capture());
        assertThat(captor.getValue().getReplyResolved()).isTrue();

        ArgumentCaptor<ActionDO> auditCaptor = ArgumentCaptor.forClass(ActionDO.class);
        verify(actions).insert(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getMetadata().get("field").asText()).isEqualTo("reply_resolved");
        assertThat(auditCaptor.getValue().getMetadata().get("to").asBoolean()).isTrue();
    }

    @Test
    void updateKol_clearsReplyResolved() {
        KolDO existing = ownedKol("Alice");
        existing.setReplyResolved(true);
        when(kols.selectById(kolId)).thenReturn(existing);

        var result = service.updateKol(userId, kolId, new KolUpdateRequest(null, null, false));

        assertThat(result.replyResolved()).isFalse();
        ArgumentCaptor<KolDO> captor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(captor.capture());
        assertThat(captor.getValue().getReplyResolved()).isFalse();
    }

    @Test
    void updateKol_rejectsBlankName() {
        KolDO existing = ownedKol("Alice");
        when(kols.selectById(kolId)).thenReturn(existing);

        assertThatThrownBy(() -> service.updateKol(userId, kolId, new KolUpdateRequest("  ", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void updateKol_rejectsEmptyPatch() {
        KolDO existing = ownedKol("Alice");
        when(kols.selectById(kolId)).thenReturn(existing);

        assertThatThrownBy(() -> service.updateKol(userId, kolId, new KolUpdateRequest(null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void updateKol_rejectsNonOwner() {
        KolDO existing = ownedKol("Alice");
        existing.setOwnerUserId(UUID.randomUUID());
        when(kols.selectById(kolId)).thenReturn(existing);

        assertThatThrownBy(() -> service.updateKol(userId, kolId, new KolUpdateRequest("Bob", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void updateKol_allowsUnownedKol() {
        KolDO existing = ownedKol("Alice");
        existing.setOwnerUserId(null);
        when(kols.selectById(kolId)).thenReturn(existing);

        var result = service.updateKol(userId, kolId, new KolUpdateRequest("Bob", null, null));

        assertThat(result.name()).isEqualTo("Bob");
    }

    @Test
    void updateKol_notFound() {
        when(kols.selectById(kolId)).thenReturn(null);

        assertThatThrownBy(() -> service.updateKol(userId, kolId, new KolUpdateRequest("Bob", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo("NOT_FOUND");
    }

    private KolDO ownedKol(String name) {
        KolDO kol = new KolDO();
        kol.setId(kolId);
        kol.setOwnerUserId(userId);
        kol.setName(name);
        kol.setEmail("alice@example.com");
        return kol;
    }
}
