package com.lovart.maildesk.application.sync.feishu;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuDateParserTest {

    @Test
    void parsesIsoDate() {
        assertThat(FeishuDateParser.parseFeishuDate("2026-03-15", "3月"))
                .isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void parsesChineseDate() {
        assertThat(FeishuDateParser.parseFeishuDate("2026年3月15日", "3月"))
                .isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void parsesMonthOnlyWithSheetYear() {
        assertThat(FeishuDateParser.parseFeishuDate("3月", "2026-6月"))
                .isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void parsesMdWithFallbackYearFromSheetTitle() {
        assertThat(FeishuDateParser.parseFeishuDate("3-15", "2026-6月"))
                .isEqualTo(LocalDate.of(2026, 3, 15));
    }
}
