package com.lovart.maildesk.common.feishu;

import com.lovart.maildesk.common.enums.KolStage;

/**
 * Maps granular Feishu sheet stage text into the {@link KolStage} funnel.
 * <p>
 * Ported from legacy {@code mapFeishuStage()} in {@code lib/feishu/sync-kols.ts}.
 * Administrative markers return {@code null} so existing manual stages are preserved on upsert.
 */
public final class FeishuStageMapper {

    private FeishuStageMapper() {
    }

    /**
     * @return mapped stage, or {@code null} when the Feishu value is empty, administrative,
     *         or unknown (preserve existing DB stage).
     */
    public static KolStage mapFeishuStage(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String text = rawValue.replaceAll("\\s+", "");
        if (text.isEmpty()) {
            return null;
        }

        // Non-pipeline / administrative markers → leave existing stage untouched.
        if (text.contains("未合作过") || text.contains("移至") || (text.contains("转") && text.contains("月"))) {
            return null;
        }

        // Terminal: 已拒绝 / 剔除合作 / 放弃合作.
        if (text.contains("拒绝") || text.contains("剔除") || text.contains("放弃")) {
            return KolStage.DECLINED;
        }

        // Publish + payment (check 已发布待付款 before 已付款).
        if (text.contains("发布待付款")) {
            return KolStage.PUBLISHED;
        }
        if (text.contains("已付款")) {
            return KolStage.PAYING;
        }

        // Reviewed / awaiting publish.
        if (text.contains("已审核待发布") || text.contains("待发布")) {
            return KolStage.REVIEWING;
        }

        // Content production.
        if (text.contains("脚本") || text.contains("初稿") || text.contains("视频")) {
            return KolStage.PRODUCING;
        }

        // Confirmed cooperation.
        if (text.contains("待签合同") || text.contains("价格确定") || text.contains("已合作")) {
            return KolStage.CONFIRMED;
        }

        // Creator replied / price discussion.
        if (text.contains("议价")) {
            return KolStage.NEGOTIATING;
        }

        // Outbound ask / follow-up — not a creator reply.
        if (text.contains("询价") || text.contains("追")) {
            return KolStage.OUTREACH;
        }

        // Previously collaborated KOLs worth re-touching.
        if (text.contains("合作过")) {
            return KolStage.REINVEST;
        }

        return null;
    }
}
