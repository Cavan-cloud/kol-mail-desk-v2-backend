package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.integration.feishu.FeishuProperties;

final class FeishuSyncTestSupport {

    private FeishuSyncTestSupport() {
    }

    static FeishuProperties properties(String tabFilter, boolean fullSync) {
        FeishuProperties properties = new FeishuProperties();
        properties.setTabFilter(tabFilter);
        properties.setRegionalTabNames("欧美,拉美,韩国");
        properties.setFullSync(fullSync);
        properties.setSheetRecentMonths(2);
        return properties;
    }

    static FeishuProperties rosterFullSyncProperties() {
        return properties("roster", true);
    }

    static FeishuProperties recentMonthsProperties() {
        return properties("recent-months", true);
    }
}
