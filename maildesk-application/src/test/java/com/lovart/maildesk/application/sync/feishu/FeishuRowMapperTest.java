package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.enums.KolStage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuRowMapperTest {

    private static final List<String> HEADER = List.of(
            "KOL用户名", "联系方式", "账号（主页链接）", "主平台", "频道类型", "运营", "报价", "状态", "建联时间", "备注");

    @Test
    void resolvesColumnsAndMapsFullRow() {
        FeishuColumnIndex columns = FeishuColumnResolver.resolve(HEADER, FeishuFieldHeaders.defaults());
        List<Object> row = List.of(
                "Alice",
                "alice@example.com",
                "https://www.tiktok.com/@alice",
                "TikTok",
                "美妆",
                "@Bob",
                "USD 1200",
                "议价中",
                "2026-03-15",
                "重点跟进");

        FeishuKolDraft draft = FeishuRowMapper.mapRow(row, columns, "2026-3月")
                .orElseThrow();

        assertThat(draft.email()).isEqualTo("alice@example.com");
        assertThat(draft.operatorName()).isEqualTo("Bob");
        assertThat(draft.name()).isEqualTo("Alice");
        assertThat(draft.displayName()).isEqualTo("Alice");
        assertThat(draft.profileUrl()).isEqualTo("https://www.tiktok.com/@alice");
        assertThat(draft.primaryPlatform()).isEqualTo("tiktok");
        assertThat(draft.type()).isEqualTo("美妆");
        assertThat(draft.brandQuote()).isEqualTo("USD 1200");
        assertThat(draft.finalCooperationPrice()).isNull();
        assertThat(draft.agreedPrice()).isEqualByComparingTo(new BigDecimal("1200"));
        assertThat(draft.stage()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(draft.feishuOutreachAt()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(draft.notes()).contains("合作进展: 议价中");
    }

    @Test
    void skipsRowWithoutValidEmail() {
        FeishuColumnIndex columns = FeishuColumnResolver.resolve(HEADER, FeishuFieldHeaders.defaults());
        List<Object> row = List.of("Alice", "not-an-email", "", "", "", "@Bob", "", "已询价", "", "");

        assertThat(FeishuRowMapper.mapRow(row, columns, "3月")).isEmpty();
    }

    @Test
    void administrativeStageLeavesNullStage() {
        FeishuColumnIndex columns = FeishuColumnResolver.resolve(HEADER, FeishuFieldHeaders.defaults());
        List<Object> row = List.of("Alice", "alice@example.com", "", "", "", "@Bob", "", "未合作过", "", "");

        FeishuKolDraft draft = FeishuRowMapper.mapRow(row, columns, "3月").orElseThrow();

        assertThat(draft.stage()).isNull();
    }

    @Test
    void columnResolver_prefersExactHeaderMatch() {
        FeishuColumnIndex columns = FeishuColumnResolver.resolve(HEADER, FeishuFieldHeaders.defaults());

        assertThat(columns.email()).isEqualTo(1);
        assertThat(columns.operator()).isEqualTo(5);
        assertThat(columns.stage()).isEqualTo(7);
    }

    @Test
    void parsePrice_handlesCurrencyPrefixes() {
        assertThat(FeishuRowMapper.parsePrice("¥5,000.50")).isEqualByComparingTo(new BigDecimal("5000.50"));
        assertThat(FeishuRowMapper.parsePrice("")).isNull();
    }

    @Test
    void fallsBackToKolQuoteWhenBrandQuoteCellBlank() {
        List<String> header = List.of(
                "KOL用户名", "联系方式", "运营", "品牌报价", "KOL报价($)", "最终合作价格", "状态");
        FeishuColumnIndex columns = FeishuColumnResolver.resolve(header, FeishuFieldHeaders.defaults());
        assertThat(columns.brandQuote()).isEqualTo(3);
        assertThat(columns.kolQuote()).isEqualTo(4);
        assertThat(columns.finalCooperationPrice()).isEqualTo(5);

        List<Object> row = List.of("Alice", "alice@example.com", "@Bob", "", "900", "1100", "议价中");
        FeishuKolDraft draft = FeishuRowMapper.mapRow(row, columns, "7月").orElseThrow();

        assertThat(draft.brandQuote()).isEqualTo("900");
        assertThat(draft.finalCooperationPrice()).isEqualByComparingTo(new BigDecimal("1100"));
        assertThat(draft.agreedPrice()).isEqualByComparingTo(new BigDecimal("1100"));
    }

    @Test
    void mapsFinalQuoteHeaderWithoutStealingIntoBrandQuote() {
        List<String> header = List.of("联系方式", "运营", "报价", "最终报价");
        FeishuColumnIndex columns = FeishuColumnResolver.resolve(header, FeishuFieldHeaders.defaults());

        assertThat(columns.brandQuote()).isEqualTo(2);
        assertThat(columns.finalCooperationPrice()).isEqualTo(3);

        List<Object> row = List.of("alice@example.com", "@Bob", "800", "1000");
        FeishuKolDraft draft = FeishuRowMapper.mapRow(row, columns, "7月").orElseThrow();

        assertThat(draft.brandQuote()).isEqualTo("800");
        assertThat(draft.finalCooperationPrice()).isEqualByComparingTo(new BigDecimal("1000"));
    }
}
