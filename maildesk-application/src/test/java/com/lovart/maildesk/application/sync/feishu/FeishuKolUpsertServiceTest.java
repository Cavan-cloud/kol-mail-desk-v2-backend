package com.lovart.maildesk.application.sync.feishu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeishuKolUpsertServiceTest {

    @Mock
    private KolMapper kols;

    private FeishuKolUpsertService service;
    private OffsetDateTime now;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        service = new FeishuKolUpsertService(kols);
        now = OffsetDateTime.now(ZoneOffset.UTC);
        ownerId = UUID.randomUUID();
    }

    @Test
    void skipsManualSourceRows() {
        FeishuKolDraft draft = sampleDraft(KolStage.OUTREACH);
        KolDO existing = new KolDO();
        existing.setId(UUID.randomUUID());
        existing.setSource("manual");
        when(kols.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        FeishuKolUpsertService.UpsertStats stats = service.upsertChunk(
                List.of(draft), "sheet_token", Map.of(), now);

        assertThat(stats.upserted()).isZero();
        assertThat(stats.skipped()).isEqualTo(1);
        verify(kols, never()).updateById(any(KolDO.class));
        verify(kols, never()).insert(any(KolDO.class));
    }

    @Test
    void insertsNewFeishuRowWithOwnerMatch() {
        FeishuKolDraft draft = sampleDraft(KolStage.NEGOTIATING);
        when(kols.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        FeishuKolUpsertService.UpsertStats stats = service.upsertChunk(
                List.of(draft),
                "sheet_token",
                Map.of("bob", ownerId),
                now);

        assertThat(stats.upserted()).isEqualTo(1);
        assertThat(stats.ownerMatched()).isEqualTo(1);

        ArgumentCaptor<KolDO> captor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).insert(captor.capture());
        KolDO inserted = captor.getValue();
        assertThat(inserted.getEmail()).isEqualTo("alice@example.com");
        assertThat(inserted.getSource()).isEqualTo("feishu");
        assertThat(inserted.getStage()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(inserted.getOwnerUserId()).isEqualTo(ownerId);
        assertThat(inserted.getFeishuTableId()).isEqualTo("sheet_token");
    }

    @Test
    void updateWithoutStageLeavesStageUnsetOnPatch() {
        FeishuKolDraft draft = new FeishuKolDraft(
                "alice@example.com",
                "Bob",
                "Alice",
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 1, 1),
                null);

        KolDO existing = new KolDO();
        existing.setId(UUID.randomUUID());
        existing.setSource("feishu");
        existing.setStage(KolStage.CONFIRMED);
        when(kols.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.upsertChunk(List.of(draft), "sheet_token", Map.of(), now);

        ArgumentCaptor<KolDO> captor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).updateById(captor.capture());
        assertThat(captor.getValue().getStage()).isNull();
    }

    private static FeishuKolDraft sampleDraft(KolStage stage) {
        return new FeishuKolDraft(
                "alice@example.com",
                "Bob",
                "Alice",
                null,
                "tiktok",
                "Alice",
                "美妆",
                new BigDecimal("100"),
                stage,
                LocalDate.of(2026, 3, 1),
                "notes");
    }
}
