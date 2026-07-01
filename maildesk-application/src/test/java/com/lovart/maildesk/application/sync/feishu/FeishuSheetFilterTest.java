package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

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
}
