package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.domain.feishu.FeishuBitableTableMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuBitableTableFilterTest {

    @Test
    void keepsAllMonthTabsAndRegionalRosters() {
        List<FeishuBitableTableMeta> tables = List.of(
                new FeishuBitableTableMeta("t1", "7月"),
                new FeishuBitableTableMeta("t2", "2月"),
                new FeishuBitableTableMeta("t3", "欧美"),
                new FeishuBitableTableMeta("t4", "拉美"),
                new FeishuBitableTableMeta("t5", "韩国"),
                new FeishuBitableTableMeta("t6", "设计组项目"),
                new FeishuBitableTableMeta("t7", "Xl"));

        List<FeishuBitableTableMeta> filtered = FeishuBitableTableFilter.filterSyncTables(
                tables, Set.of("欧美", "拉美", "韩国"));

        assertThat(filtered).extracting(FeishuBitableTableMeta::name)
                .containsExactly("7月", "2月", "欧美", "拉美", "韩国");
    }
}
