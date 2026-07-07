package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.lovart.maildesk.common.feishu.FeishuTabFilterMode;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuSheetFilterTest {

    @Test
    void keepsSheetsWithoutMonthInTitle() {
        List<FeishuSheetMeta> sheets = List.of(
                new FeishuSheetMeta("sh1", "欧美", 10, 5),
                new FeishuSheetMeta("sh2", "3月", 10, 5));

        List<FeishuSheetMeta> filtered = FeishuSheetFilter.filterRecentMonthSheets(sheets, 2);

        assertThat(filtered).extracting(FeishuSheetMeta::title).contains("欧美");
    }

    @Test
    void allowedMonths_coversRecentWindow() {
        var allowed = FeishuSheetFilter.allowedMonths(2, LocalDate.of(2026, 7, 1));
        assertThat(allowed).containsExactlyInAnyOrder(7, 6);
    }

    @Test
    void roster_keepsAllMonthTabsAndRegionalSheetsOnly() {
        List<FeishuSheetMeta> sheets = List.of(
                new FeishuSheetMeta("sh7", "7月", 10, 5),
                new FeishuSheetMeta("sh2", "2月", 10, 5),
                new FeishuSheetMeta("shEu", "欧美", 10, 5),
                new FeishuSheetMeta("shLa", "拉美", 10, 5),
                new FeishuSheetMeta("shKr", "韩国", 10, 5),
                new FeishuSheetMeta("shX", "设计师项目", 10, 5));

        List<FeishuSheetMeta> filtered = FeishuSheetFilter.filterSheets(
                sheets, FeishuTabFilterMode.ROSTER, 2, Set.of("欧美", "拉美", "韩国"));

        assertThat(filtered).extracting(FeishuSheetMeta::title)
                .containsExactly("7月", "2月", "欧美", "拉美", "韩国");
    }
}
