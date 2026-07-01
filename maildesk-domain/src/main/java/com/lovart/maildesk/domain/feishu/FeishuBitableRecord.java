package com.lovart.maildesk.domain.feishu;

import java.util.Map;

/**
 * One row from Feishu Bitable ({@code record_id} + typed field map).
 */
public record FeishuBitableRecord(
        String recordId,
        Map<String, Object> fields
) {
}
