package com.lovart.maildesk.application.sync.gmail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GmailSyncOptionsTest {

    @Test
    void incremental_defaults() {
        GmailSyncOptions options = GmailSyncOptions.incremental();
        assertThat(options.mode()).isEqualTo("incremental");
        assertThat(options.maxResults()).isEqualTo(50);
        assertThat(options.historyMode()).isFalse();
    }

    @Test
    void historyPage_enablesRecentSweep() {
        GmailSyncOptions options = GmailSyncOptions.historyPage("token-1");
        assertThat(options.mode()).isEqualTo("history");
        assertThat(options.pageToken()).isEqualTo("token-1");
        assertThat(options.forceRecent()).isTrue();
        assertThat(options.historyDays()).isEqualTo(365);
    }
}
