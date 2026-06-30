package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/**
 * Parses board {@code window} query values from the OpenAPI contract.
 */
public record BoardWindow(String raw, LocalDate startInclusive, LocalDate endExclusive) {

    public static BoardWindow parse(String window) {
        String raw = window == null || window.isBlank() ? "all" : window.trim();
        if ("all".equalsIgnoreCase(raw)) {
            return new BoardWindow("all", null, null);
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if ("week".equalsIgnoreCase(raw) || "this_week".equalsIgnoreCase(raw)) {
            LocalDate start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            return new BoardWindow(raw, start, start.plusWeeks(1));
        }
        if ("month".equalsIgnoreCase(raw) || "this_month".equalsIgnoreCase(raw)) {
            LocalDate start = today.withDayOfMonth(1);
            return new BoardWindow(raw, start, start.plusMonths(1));
        }
        if ("last30".equalsIgnoreCase(raw) || "last_30_days".equalsIgnoreCase(raw)) {
            return new BoardWindow(raw, today.minusDays(30), today.plusDays(1));
        }
        if (raw.matches("\\d{4}-\\d{2}")) {
            YearMonth ym = YearMonth.parse(raw);
            return new BoardWindow(raw, ym.atDay(1), ym.plusMonths(1).atDay(1));
        }
        throw new BusinessException("VALIDATION_ERROR", "无效的时间窗参数：" + raw);
    }

    public boolean matches(LocalDate feishuOutreachAt) {
        if (startInclusive == null) {
            return true;
        }
        if (feishuOutreachAt == null) {
            return false;
        }
        return !feishuOutreachAt.isBefore(startInclusive) && feishuOutreachAt.isBefore(endExclusive);
    }
}
