package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.domain.feishu.FeishuBitableTableMeta;

import java.util.List;
import java.util.Set;

/**
 * Selects Bitable tables that participate in KOL sync: month tabs plus regional rosters.
 */
public final class FeishuBitableTableFilter {

    private FeishuBitableTableFilter() {
    }

    public static List<FeishuBitableTableMeta> filterSyncTables(
            List<FeishuBitableTableMeta> tables, Set<String> regionalTabNames) {
        if (tables == null || tables.isEmpty()) {
            return List.of();
        }
        return tables.stream()
                .filter(table -> FeishuSheetFilter.isRosterTab(table.name(), regionalTabNames))
                .toList();
    }
}
