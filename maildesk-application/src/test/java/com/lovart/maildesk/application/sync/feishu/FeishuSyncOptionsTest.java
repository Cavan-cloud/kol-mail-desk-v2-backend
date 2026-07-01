package com.lovart.maildesk.application.sync.feishu;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuSyncOptionsTest {

    @Test
    void deltaBatch_capsMergedPairsPerRun() {
        FeishuSyncOptions options = FeishuSyncOptions.deltaBatch(50);

        assertThat(options.maxRecords()).isEqualTo(50);
        assertThat(options.dryRun()).isFalse();
        assertThat(options.sheetId()).isNull();
        assertThat(options.recentMonths()).isEqualTo(FeishuSyncOptions.DEFAULT_RECENT_MONTHS);
    }

    @Test
    void backfill_scansAllSheetsWithoutRowCap() {
        FeishuSyncOptions options = FeishuSyncOptions.backfill(0, false);

        assertThat(options.maxRecords()).isNull();
        assertThat(options.recentMonths()).isZero();
        assertThat(options.dryRun()).isFalse();
    }

    @Test
    void backfill_supportsDryRunAndRecentWindow() {
        FeishuSyncOptions options = FeishuSyncOptions.backfill(2, true);

        assertThat(options.recentMonths()).isEqualTo(2);
        assertThat(options.dryRun()).isTrue();
    }
}
