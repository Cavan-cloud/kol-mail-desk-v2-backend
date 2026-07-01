package com.lovart.maildesk.common.feishu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuCellExtractorTest {

    @Test
    void extractEmail_fromPlainText() {
        assertThat(FeishuCellExtractor.extractEmail("kol@example.com")).isEqualTo("kol@example.com");
        assertThat(FeishuCellExtractor.extractEmail("mailto:kol@example.com")).isEqualTo("kol@example.com");
    }

    @Test
    void extractEmail_fromRichObject() {
        assertThat(FeishuCellExtractor.extractEmail(Map.of("text", "kol@example.com"))).isEqualTo("kol@example.com");
    }

    @Test
    void extractOperatorNames_splitsMergedMentions() {
        assertThat(FeishuCellExtractor.extractOperatorNames("@张三@李四"))
                .containsExactly("张三", "李四");
    }

    @Test
    void normalizeOperatorName_stripsAtAndWhitespace() {
        assertThat(FeishuCellExtractor.normalizeOperatorName(" @张 三 ")).isEqualTo("张三");
    }

    @Test
    void mergeKey_usesNormalizedEmailAndOperator() {
        assertThat(FeishuCellExtractor.mergeKey("KOL@Example.COM", "@张三"))
                .isEqualTo("kol@example.com|张三");
    }

    @Test
    void extractUrl_prefersHttpLink() {
        assertThat(FeishuCellExtractor.extractUrl("主页 https://tiktok.com/@kol 其他"))
                .isEqualTo("https://tiktok.com/@kol");
    }

    @Test
    void extractText_fromNestedList() {
        assertThat(FeishuCellExtractor.extractText(List.of("A", "B"))).isEqualTo("A, B");
    }
}
