package com.lovart.maildesk.application.sync.feishu;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Feishu sheet date cells into {@link LocalDate} cohort anchors.
 * Ported from legacy {@code parseFeishuDate()} in {@code sync-kols.ts}.
 */
public final class FeishuDateParser {

    private static final Pattern YEAR_IN_TEXT = Pattern.compile("(20\\d{2})");
    private static final Pattern YMD = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern YM = Pattern.compile("^(\\d{4})-(\\d{1,2})$");
    private static final Pattern MD = Pattern.compile("^(\\d{1,2})-(\\d{1,2})$");
    private static final Pattern MONTH_ONLY = Pattern.compile("^(\\d{1,2})-?$");
    private static final Pattern SERIAL = Pattern.compile("^\\d{5}$");

    private FeishuDateParser() {
    }

    public static LocalDate parseFeishuDate(String rawValue, String sheetTitleFallback) {
        if (rawValue == null) {
            return null;
        }
        String raw = rawValue.trim();
        if (raw.isEmpty()) {
            return null;
        }

        String normalized = raw.replaceAll("\\s+", "")
                .replaceAll("[年月./]", "-")
                .replace("日", "");

        Matcher ymd = YMD.matcher(normalized);
        if (ymd.find()) {
            return toLocalDate(Integer.parseInt(ymd.group(1)), Integer.parseInt(ymd.group(2)), Integer.parseInt(ymd.group(3)));
        }

        Matcher ym = YM.matcher(normalized);
        if (ym.find()) {
            return toLocalDate(Integer.parseInt(ym.group(1)), Integer.parseInt(ym.group(2)), 1);
        }

        String fallbackYear = extractYear(sheetTitleFallback);
        if (fallbackYear == null) {
            fallbackYear = String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear());
        }

        Matcher md = MD.matcher(normalized);
        if (md.find()) {
            return toLocalDate(Integer.parseInt(fallbackYear), Integer.parseInt(md.group(1)), Integer.parseInt(md.group(2)));
        }

        Matcher monthOnly = MONTH_ONLY.matcher(normalized);
        if (monthOnly.find() && (raw.contains("月") || raw.contains("月份"))) {
            return toLocalDate(Integer.parseInt(fallbackYear), Integer.parseInt(monthOnly.group(1)), 1);
        }

        if (SERIAL.matcher(raw).matches()) {
            return excelSerialToLocalDate(Integer.parseInt(raw));
        }

        if (raw.matches(".*\\d{4}.*")) {
            try {
                return LocalDate.parse(raw.substring(0, Math.min(raw.length(), 10)));
            } catch (RuntimeException ignored) {
                // fall through
            }
        }
        return null;
    }

    static LocalDate excelSerialToLocalDate(int serial) {
        if (serial < 30_000 || serial > 80_000) {
            return null;
        }
        long epochDay = serial - 25_569L;
        return LocalDate.ofEpochDay(epochDay);
    }

    private static LocalDate toLocalDate(int year, int month, int day) {
        if (year < 2000 || month < 1 || month > 12 || day < 1 || day > 31) {
            return null;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String extractYear(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = YEAR_IN_TEXT.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
