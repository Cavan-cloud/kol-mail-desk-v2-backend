package com.lovart.maildesk.application.sync.feishu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.domain.feishu.FeishuClient;
import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeishuSyncServiceTest {

    @Mock
    private FeishuClient feishuClient;

    @Mock
    private ProfileMapper profiles;

    @Mock
    private KolMapper kols;

    private FeishuSyncService service;

    @BeforeEach
    void setUp() {
        service = new FeishuSyncService(
                feishuClient,
                FeishuSyncTestSupport.recentMonthsProperties(),
                profiles,
                new FeishuKolUpsertService(kols));
    }

    @Test
    void returnsNotConfiguredWhenFeishuMissing() {
        when(feishuClient.isConfigured()).thenReturn(false);

        FeishuSyncResult result = service.sync();

        assertThat(result.status()).isEqualTo("not_configured");
    }

    @Test
    void parsesSheetRowsAndUpsertsMergedPairs() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.isBitableSource()).thenReturn(false);
        when(feishuClient.configuredKolAppToken()).thenReturn("sheet_token");
        when(feishuClient.listSheets()).thenReturn(List.of(new FeishuSheetMeta("sh1", "欧美", 5, 10)));
        when(feishuClient.readSheetValues(any(FeishuSheetMeta.class))).thenReturn(List.of(
                List.of("KOL用户名", "联系方式", "主平台", "频道类型", "运营", "报价", "状态", "建联时间"),
                List.of("Alice", "alice@example.com", "TikTok", "美妆", "@Bob", "USD 500", "已询价", "2026-03-01")));
        when(profiles.selectList(any())).thenReturn(List.of());
        when(kols.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        FeishuSyncResult result = service.sync(FeishuSyncOptions.defaults());

        assertThat(result.status()).isEqualTo("synced");
        assertThat(result.mergedPairs()).isEqualTo(1);
        assertThat(result.upserted()).isEqualTo(1);
        ArgumentCaptor<KolDO> insertCaptor = ArgumentCaptor.forClass(KolDO.class);
        verify(kols).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void dryRunDoesNotPersist() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.isBitableSource()).thenReturn(false);
        when(feishuClient.configuredKolAppToken()).thenReturn("sheet_token");
        when(feishuClient.listSheets()).thenReturn(List.of(new FeishuSheetMeta("sh1", "欧美", 5, 10)));
        when(feishuClient.readSheetValues(any(FeishuSheetMeta.class))).thenReturn(List.of(
                List.of("KOL用户名", "联系方式", "主平台", "频道类型", "运营", "报价", "状态", "建联时间"),
                List.of("Alice", "alice@example.com", "", "", "", "", "已询价", "")));
        when(profiles.selectList(any())).thenReturn(List.of());

        FeishuSyncResult result = service.sync(new FeishuSyncOptions(null, null, true, null, 2, List.of("欧美")));

        assertThat(result.dryRun()).isTrue();
        assertThat(result.upserted()).isEqualTo(1);
        verifyNoInteractions(kols);
    }
}
