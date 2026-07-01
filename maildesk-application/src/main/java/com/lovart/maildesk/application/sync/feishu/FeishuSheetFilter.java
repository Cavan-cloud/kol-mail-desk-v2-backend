package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filters spreadsheet tabs by encoded month title (e.g. {@code 3月}).
 * Ported from legacy {@code filterRecentMonthSheets()} in {@code sync-kols.ts}.
 */
public final class FeishuSheetFilter {

    private static final Pattern MONTH_IN_TITLE = Pattern.compile("(\\d{1,2})\\s*月");

    private FeishuSheetFilter() {
    }

    public static List<FeishuSheetMeta> filterRecentMonthSheets(List<FeishuSheetMeta> sheets, int monthsWindow) {
        if (monthsWindow <= 0) {
            return sheets;
        }
        Set<Integer> allowed = allowedMonths(monthsWindow, LocalDate.now());
        return sheets.stream()
                .filter(sheet -> {
                    Integer month = parseMonthFromTitle(sheet.title());
                    return month == null || allowed.contains(month);
                })
                .toList();
    }

    static Set<Integer> allowedMonths(int monthsWindow, LocalDate today) {
        int current = today.getMonthValue();
        Set<Integer> allowed = new HashSet<>();
        for (int i = 0; i < monthsWindow; i++) {
            int month = ((current - 1 - i) % 12 + 12) % 12 + 1;
            allowed.add(month);
        }
        return allowed;
    }

    static Integer parseMonthFromTitle(String title) {
        if (title == null) {
            return null;
        }
        Matcher matcher = MONTH_IN_TITLE.matcher(title);
        if (!matcher.find()) {
            return null;
        }
        int month = Integer.parseInt(matcher.group(1));
        if (month < 1 || month > 12) {
            return null;
        }
        return month;
    }
}
