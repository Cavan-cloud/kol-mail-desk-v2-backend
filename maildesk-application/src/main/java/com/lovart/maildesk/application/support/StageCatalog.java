package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.enums.KolStage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage ordering and Chinese labels aligned with {@code lib/domain.ts}.
 */
public final class StageCatalog {

    public static final List<KolStage> FUNNEL_STAGES = List.of(
            KolStage.OUTREACH,
            KolStage.REPLIED,
            KolStage.NEGOTIATING,
            KolStage.CONFIRMED,
            KolStage.PRODUCING,
            KolStage.REVIEWING,
            KolStage.PUBLISHED,
            KolStage.PAYING
    );

    public static final List<KolStage> ALL_STAGES = List.of(
            KolStage.OUTREACH,
            KolStage.REPLIED,
            KolStage.NEGOTIATING,
            KolStage.CONFIRMED,
            KolStage.PRODUCING,
            KolStage.REVIEWING,
            KolStage.PUBLISHED,
            KolStage.PAYING,
            KolStage.REINVEST,
            KolStage.DECLINED
    );

    private static final Map<KolStage, String> LABELS = Map.ofEntries(
            Map.entry(KolStage.OUTREACH, "触达"),
            Map.entry(KolStage.REPLIED, "回复"),
            Map.entry(KolStage.NEGOTIATING, "沟通 / 议价"),
            Map.entry(KolStage.CONFIRMED, "确认合作"),
            Map.entry(KolStage.PRODUCING, "制作中"),
            Map.entry(KolStage.REVIEWING, "审稿 / 待发布"),
            Map.entry(KolStage.PUBLISHED, "发布"),
            Map.entry(KolStage.PAYING, "付款"),
            Map.entry(KolStage.REINVEST, "复投"),
            Map.entry(KolStage.DECLINED, "已拒绝")
    );

    private StageCatalog() {
    }

    public static String label(KolStage stage) {
        return LABELS.getOrDefault(stage, stage.name().toLowerCase());
    }

    public static String jsonStage(KolStage stage) {
        return stage == null ? null : stage.name().toLowerCase();
    }

    public static int cumulativeCount(Map<KolStage, Integer> snapshot, KolStage stage) {
        int idx = FUNNEL_STAGES.indexOf(stage);
        if (idx < 0) {
            return snapshot.getOrDefault(stage, 0);
        }
        int sum = 0;
        for (int i = idx; i < FUNNEL_STAGES.size(); i++) {
            sum += snapshot.getOrDefault(FUNNEL_STAGES.get(i), 0);
        }
        return sum;
    }

    public static Map<String, Integer> stageCountsJson(Map<KolStage, Integer> counts) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (KolStage stage : ALL_STAGES) {
            out.put(jsonStage(stage), counts.getOrDefault(stage, 0));
        }
        return out;
    }
}
