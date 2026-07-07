package com.lovart.maildesk.domain.feishu;

/**
 * One data table inside a Feishu Bitable app (e.g. {@code 7月}, {@code 欧美}).
 */
public record FeishuBitableTableMeta(
        String tableId,
        String name
) {
}
