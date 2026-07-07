package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.domain.feishu.FeishuBitableRecord;
import com.lovart.maildesk.domain.feishu.FeishuBitableTableMeta;
import com.lovart.maildesk.domain.feishu.FeishuClient;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeishuSyncServiceBitableTest {

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
    void syncBitable_scansMonthAndRegionalTablesOnly() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.isBitableSource()).thenReturn(true);
        when(feishuClient.configuredKolAppToken()).thenReturn("app_token");
        when(feishuClient.listBitableTables()).thenReturn(List.of(
                new FeishuBitableTableMeta("tbl7", "7月"),
                new FeishuBitableTableMeta("tbl6", "6月"),
                new FeishuBitableTableMeta("tblEu", "欧美"),
                new FeishuBitableTableMeta("tblX", "设计组项目")));
        when(feishuClient.listBitableRecords(eq("app_token"), eq("tbl7"), any()))
                .thenReturn(List.of(new FeishuBitableRecord(
                        "rec1",
                        Map.of(
                                "KOL联系方式", "alice@example.com",
                                "运营", "@Bob",
                                "KOL账户名", "Alice",
                                "状态", "已询价"))));
        when(feishuClient.listBitableRecords(eq("app_token"), eq("tbl6"), any())).thenReturn(List.of());
        when(feishuClient.listBitableRecords(eq("app_token"), eq("tblEu"), any()))
                .thenReturn(List.of(new FeishuBitableRecord(
                        "rec2",
                        Map.of("KOL联系方式", "eve@example.com", "状态", "合作过"))));
        when(profiles.selectList(any())).thenReturn(List.of());
        when(kols.selectOne(any())).thenReturn(null);

        FeishuSyncResult result = service.sync(FeishuSyncOptions.defaults());

        assertThat(result.status()).isEqualTo("synced");
        assertThat(result.sheetsScanned()).isEqualTo(3);
        assertThat(result.mergedPairs()).isEqualTo(2);
        assertThat(result.skippedSheets())
                .anyMatch(s -> "设计组项目".equals(s.title()) && "not a roster tab (month or regional)".equals(s.reason()));
        verify(kols, org.mockito.Mockito.times(2)).insert(any(KolDO.class));
    }

    @Test
    void syncBitable_dryRunDoesNotPersist() {
        when(feishuClient.isConfigured()).thenReturn(true);
        when(feishuClient.isBitableSource()).thenReturn(true);
        when(feishuClient.configuredKolAppToken()).thenReturn("app_token");
        when(feishuClient.listBitableTables()).thenReturn(List.of(new FeishuBitableTableMeta("tbl7", "7月")));
        when(feishuClient.listBitableRecords(eq("app_token"), eq("tbl7"), any()))
                .thenReturn(List.of(new FeishuBitableRecord(
                        "rec1",
                        Map.of("KOL联系方式", "alice@example.com", "运营", "@Bob"))));
        when(profiles.selectList(any())).thenReturn(List.of());

        FeishuSyncResult result = service.sync(new FeishuSyncOptions(null, null, true, null, 2, List.of("欧美")));

        assertThat(result.dryRun()).isTrue();
        assertThat(result.mergedPairs()).isEqualTo(1);
        verifyNoInteractions(kols);
    }
}
