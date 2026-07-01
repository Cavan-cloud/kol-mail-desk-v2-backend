package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.application.dto.FeishuSyncStatusDto;
import com.lovart.maildesk.domain.feishu.FeishuClient;
import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeishuSyncApplicationServiceTest {

    @Mock
    private FeishuClient feishuClient;

    @Mock
    private ProfileMapper profiles;

    @Mock
    private KolMapper kols;

    private FeishuSyncApplicationService service;

    @BeforeEach
    void setUp() {
        FeishuSyncService syncService =
                new FeishuSyncService(feishuClient, profiles, new FeishuKolUpsertService(kols));
        service = new FeishuSyncApplicationService(syncService);
    }

    @Test
    void triggerSync_mapsUpsertCount() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.configuredKolAppToken()).thenReturn("sheet_token");
        when(feishuClient.listSheets()).thenReturn(List.of(new FeishuSheetMeta("sh1", "欧美", 5, 10)));
        when(feishuClient.readSheetValues(any(FeishuSheetMeta.class))).thenReturn(List.of(
                List.of("KOL用户名", "联系方式", "主平台", "频道类型", "运营", "报价", "状态", "建联时间"),
                List.of("Alice", "alice@example.com", "", "", "", "", "已询价", "")));
        when(profiles.selectList(any())).thenReturn(List.of());
        when(kols.selectOne(any())).thenReturn(null);

        FeishuSyncStatusDto status = service.triggerSync();

        assertThat(status.running()).isFalse();
        assertThat(status.upserted()).isEqualTo(1);
        assertThat(status.lastSyncedAt()).isNotNull();
    }

    @Test
    void triggerSync_mapsNotConfigured() {
        when(feishuClient.isConfigured()).thenReturn(false);

        FeishuSyncStatusDto status = service.triggerSync();

        assertThat(status.lastError()).contains("飞书");
        assertThat(status.upserted()).isZero();
    }
}
