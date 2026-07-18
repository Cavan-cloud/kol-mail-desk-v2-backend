package com.lovart.maildesk.application.sync.feishu;

import java.util.List;

/**
 * Configurable Feishu sheet header candidates per logical field.
 * Defaults mirror legacy {@code getHeaderMap()} env fallbacks in {@code sync-kols.ts}.
 */
public record FeishuFieldHeaders(
        List<String> email,
        List<String> operator,
        List<String> name,
        List<String> profileUrl,
        List<String> platform,
        List<String> country,
        List<String> language,
        List<String> type,
        List<String> followers,
        List<String> brandQuote,
        List<String> finalCooperationPrice,
        List<String> cooperation,
        List<String> finalCooperation,
        List<String> stage,
        List<String> outreachDate,
        List<String> notes) {

    public static FeishuFieldHeaders defaults() {
        return new FeishuFieldHeaders(
                List.of("KOL联系方式", "联系方式", "邮箱", "Email"),
                List.of("运营", "负责人"),
                List.of("KOL用户名", "KOL账户名", "达人名", "名称"),
                List.of("主页链接", "主页链接合集", "账号（主页链接）"),
                List.of("平台（只填主渠道）", "平台/召唤工具", "主平台", "平台"),
                List.of("国家", "注册国家"),
                List.of("语言"),
                List.of("频道类型", "类型"),
                List.of("粉丝数", "主平台粉丝数"),
                // Prefer brand-side quote; KOL报价($) / 合作报价 are cell-level fallbacks in mappers.
                List.of("品牌报价", "KOL报价($)", "合作报价（$）", "合作报价($)", "报价"),
                List.of("最终合作价格", "最终报价"),
                List.of("合作状态", "当前状态"),
                List.of("是否最终合作"),
                List.of("状态", "合作进展", "合作状态"),
                List.of(
                        "建联时间",
                        "建联日期",
                        "进款日期",
                        "KOL建联时间",
                        "KOL建联日期",
                        "首次建联时间",
                        "首次建联日期",
                        "首次触达时间",
                        "首次触达日期",
                        "触达时间",
                        "触达日期",
                        "询价时间",
                        "已询价时间",
                        "询价日期",
                        "发送时间",
                        "发送日期",
                        "创建时间",
                        "创建日期",
                        "联系时间",
                        "联系日期",
                        "日期",
                        "时间",
                        "月份",
                        "月"),
                List.of("备注"));
    }
}
