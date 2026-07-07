package com.lovart.maildesk.application.sync.feishu;

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
class FeishuSyncServiceSheetRosterTest {

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
                FeishuSyncTestSupport.rosterFullSyncProperties(),
                profiles,
                new FeishuKolUpsertService(kols));
    }

    @Test
    void syncSheet_scansAllMonthAndRegionalTabsOnly() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.isBitableSource()).thenReturn(false);
        when(feishuClient.configuredKolAppToken()).thenReturn("sheet_token");
        when(feishuClient.listSheets()).thenReturn(List.of(
                new FeishuSheetMeta("sh7", "7月", 5, 10),
                new FeishuSheetMeta("sh6", "6月", 5, 10),
                new FeishuSheetMeta("shEu", "欧美", 5, 10),
                new FeishuSheetMeta("shLa", "拉美", 5, 10),
                new FeishuSheetMeta("shKr", "韩国", 5, 10),
                new FeishuSheetMeta("shX", "设计师项目", 5, 10)));
        when(feishuClient.readSheetValues(any(FeishuSheetMeta.class))).thenReturn(List.of(
                List.of("KOL联系方式", "运营"),
                List.of("skip@example.com", "@Ops")));
        when(profiles.selectList(any())).thenReturn(List.of());
        when(kols.selectOne(any())).thenReturn(null);

        FeishuSyncResult result = service.sync(FeishuSyncOptions.defaults());

        assertThat(result.sheetsScanned()).isEqualTo(5);
        assertThat(result.skippedSheets())
                .anyMatch(s -> "设计师项目".equals(s.title()) && "not a roster tab (month or regional)".equals(s.reason()));
    }

    @Test
    void backfillWithRecentMonthsZero_stillUsesRosterFilter() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.isBitableSource()).thenReturn(false);
        when(feishuClient.configuredKolAppToken()).thenReturn("sheet_token");
        when(feishuClient.listSheets()).thenReturn(List.of(
                new FeishuSheetMeta("sh7", "7月", 5, 10),
                new FeishuSheetMeta("sh6", "6月", 5, 10),
                new FeishuSheetMeta("shEu", "欧美", 5, 10),
                new FeishuSheetMeta("shX", "设计师项目", 5, 10)));
        when(feishuClient.readSheetValues(any(FeishuSheetMeta.class))).thenReturn(List.of(
                List.of("KOL联系方式", "运营"),
                List.of("a@example.com", "@Ops")));
        when(profiles.selectList(any())).thenReturn(List.of());

        FeishuSyncResult result = service.sync(FeishuSyncOptions.backfill(0, true));

        assertThat(result.sheetsScanned()).isEqualTo(3);
        assertThat(result.dryRun()).isTrue();
    }
}
