package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.feishu.FeishuBitableRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuBitableRowMapperTest {

    @Test
    void mapsBitableFieldsToDraft() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("KOL联系方式", "alice@example.com");
        fields.put("运营", List.of(Map.of("name", "@Bob")));
        fields.put("KOL账户名", "Alice");
        fields.put("主页链接", Map.of("link", "https://www.tiktok.com/@alice"));
        fields.put("平台/召唤工具", "TikTok");
        fields.put("频道类型", "美妆");
        fields.put("KOL报价($)", 1200);
        fields.put("状态", "议价中");
        fields.put("建联日期", "2026-03-15");
        fields.put("国家", "美国");
        fields.put("备注", "重点跟进");
        FeishuBitableRecord record = new FeishuBitableRecord("rec1", fields);

        FeishuKolDraft draft = FeishuBitableRowMapper.mapRecord(record, FeishuFieldHeaders.defaults(), "7月")
                .orElseThrow();

        assertThat(draft.email()).isEqualTo("alice@example.com");
        assertThat(draft.operatorName()).isEqualTo("Bob");
        assertThat(draft.name()).isEqualTo("Alice");
        assertThat(draft.profileUrl()).isEqualTo("https://www.tiktok.com/@alice");
        assertThat(draft.primaryPlatform()).isEqualTo("tiktok");
        assertThat(draft.type()).isEqualTo("美妆");
        assertThat(draft.agreedPrice()).isEqualByComparingTo(new BigDecimal("1200"));
        assertThat(draft.stage()).isEqualTo(KolStage.NEGOTIATING);
        assertThat(draft.notes()).contains("国家: 美国");
    }

    @Test
    void skipsRecordWithoutEmail() {
        FeishuBitableRecord record = new FeishuBitableRecord("rec1", Map.of("KOL账户名", "Alice"));

        assertThat(FeishuBitableRowMapper.mapRecord(record, FeishuFieldHeaders.defaults(), "7月")).isEmpty();
    }
}
