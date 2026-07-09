package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.common.feishu.FeishuStageMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps one Feishu sheet data row into a {@link FeishuKolDraft}.
 * Ported from legacy row parsing in {@code lib/feishu/sync-kols.ts}.
 */
public final class FeishuRowMapper {

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(?:USD|CNY|RMB|[¥$])?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private FeishuRowMapper() {
    }

    /**
     * @return empty when the row has no valid email
     */
    public static Optional<FeishuKolDraft> mapRow(
            List<?> row, FeishuColumnIndex columns, String sheetTitle) {
        if (row == null || row.isEmpty()) {
            return Optional.empty();
        }

        String email = FeishuCellExtractor.extractEmail(at(row, columns.email()))
                .toLowerCase(Locale.ROOT)
                .trim();
        if (email.isEmpty() || !email.contains("@")) {
            return Optional.empty();
        }

        List<String> operatorNames = FeishuCellExtractor.extractOperatorNames(at(row, columns.operator()));
        String operatorName = operatorNames.isEmpty() ? "" : operatorNames.getFirst();

        String name = pick(row, columns.name());
        String profileUrlRaw = pick(row, columns.profileUrl());
        String profileUrl = FeishuCellExtractor.extractUrl(profileUrlRaw);
        if (profileUrl.isBlank()) {
            profileUrl = null;
        }

        String platform = FeishuPlatformNormalizer.normalizePlatform(pick(row, columns.platform()));
        if (platform == null) {
            platform = FeishuPlatformNormalizer.normalizePlatformFromUrl(profileUrlRaw);
        }

        String handle = name.isBlank() ? null : name;
        String type = blankToNull(pick(row, columns.type()));
        String brandQuote = blankToNull(pick(row, columns.brandQuote()));
        BigDecimal finalCooperationPrice = parsePrice(pick(row, columns.finalCooperationPrice()));
        KolStage stage = FeishuStageMapper.mapFeishuStage(pick(row, columns.stage()));
        var outreachAt = FeishuDateParser.parseFeishuDate(pick(row, columns.outreachDate()), sheetTitle);
        String notes = buildNotes(row, columns);

        return Optional.of(new FeishuKolDraft(
                email,
                operatorName,
                blankToNull(name),
                profileUrl,
                platform,
                handle,
                type,
                brandQuote,
                finalCooperationPrice,
                stage,
                outreachAt,
                notes));
    }

    private static String buildNotes(List<?> row, FeishuColumnIndex columns) {
        List<String> lines = new ArrayList<>();
        appendNote(lines, "合作状态", pick(row, columns.cooperation()));
        appendNote(lines, "是否最终合作", pick(row, columns.finalCooperation()));
        appendNote(lines, "合作进展", pick(row, columns.stage()));
        appendNote(lines, "品牌报价", pick(row, columns.brandQuote()));
        appendNote(lines, "最终合作价格", pick(row, columns.finalCooperationPrice()));
        appendNote(lines, "粉丝数", pick(row, columns.followers()));
        appendNote(lines, "国家", pick(row, columns.country()));
        appendNote(lines, "语言", pick(row, columns.language()));
        appendNote(lines, "备注", pick(row, columns.notes()));
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private static void appendNote(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + ": " + value);
        }
    }

    static BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace(",", "").trim();
        Matcher matcher = PRICE_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1));
    }

    private static String pick(List<?> row, Integer index) {
        return FeishuCellExtractor.extractText(at(row, index));
    }

    private static Object at(List<?> row, Integer index) {
        if (index == null || index < 0 || index >= row.size()) {
            return null;
        }
        return row.get(index);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
