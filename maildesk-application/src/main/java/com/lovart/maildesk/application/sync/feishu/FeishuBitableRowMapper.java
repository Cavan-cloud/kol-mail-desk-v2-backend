package com.lovart.maildesk.application.sync.feishu;

import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.common.feishu.FeishuStageMapper;
import com.lovart.maildesk.domain.feishu.FeishuBitableRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps one Feishu Bitable record into a {@link FeishuKolDraft}.
 */
public final class FeishuBitableRowMapper {

    private FeishuBitableRowMapper() {
    }

    /**
     * @return empty when the record has no valid email
     */
    public static Optional<FeishuKolDraft> mapRecord(
            FeishuBitableRecord record, FeishuFieldHeaders headers, String tableName) {
        if (record == null || record.fields() == null || record.fields().isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> fields = record.fields();

        String email = FeishuCellExtractor.extractEmail(
                        FeishuFieldResolver.pickFieldRaw(fields, headers.email()))
                .toLowerCase(Locale.ROOT)
                .trim();
        if (email.isEmpty() || !email.contains("@")) {
            return Optional.empty();
        }

        List<String> operatorNames = FeishuCellExtractor.extractOperatorNames(
                FeishuFieldResolver.pickFieldRaw(fields, headers.operator()));
        String operatorName = operatorNames.isEmpty() ? "" : operatorNames.getFirst();

        String name = FeishuFieldResolver.pickField(fields, headers.name());
        String profileUrlRaw = FeishuFieldResolver.pickField(fields, headers.profileUrl());
        String profileUrl = FeishuCellExtractor.extractUrl(profileUrlRaw);
        if (profileUrl.isBlank()) {
            profileUrl = null;
        }

        String platform = FeishuPlatformNormalizer.normalizePlatform(
                FeishuFieldResolver.pickField(fields, headers.platform()));
        if (platform == null) {
            platform = FeishuPlatformNormalizer.normalizePlatformFromUrl(profileUrlRaw);
        }

        String handle = name.isBlank() ? null : name;
        String type = blankToNull(FeishuFieldResolver.pickField(fields, headers.type()));
        BigDecimal price = FeishuRowMapper.parsePrice(FeishuFieldResolver.pickField(fields, headers.price()));
        KolStage stage = FeishuStageMapper.mapFeishuStage(FeishuFieldResolver.pickField(fields, headers.stage()));
        var outreachAt = FeishuDateParser.parseFeishuDate(
                FeishuFieldResolver.pickField(fields, headers.outreachDate()), tableName);
        String notes = buildNotes(fields, headers);

        return Optional.of(new FeishuKolDraft(
                email,
                operatorName,
                blankToNull(name),
                profileUrl,
                platform,
                handle,
                type,
                price,
                stage,
                outreachAt,
                notes));
    }

    private static String buildNotes(Map<String, Object> fields, FeishuFieldHeaders headers) {
        List<String> lines = new ArrayList<>();
        appendNote(lines, "合作状态", FeishuFieldResolver.pickField(fields, headers.cooperation()));
        appendNote(lines, "是否最终合作", FeishuFieldResolver.pickField(fields, headers.finalCooperation()));
        appendNote(lines, "合作进展", FeishuFieldResolver.pickField(fields, headers.stage()));
        appendNote(lines, "报价", FeishuFieldResolver.pickField(fields, headers.price()));
        appendNote(lines, "粉丝数", FeishuFieldResolver.pickField(fields, headers.followers()));
        appendNote(lines, "国家", FeishuFieldResolver.pickField(fields, headers.country()));
        appendNote(lines, "语言", FeishuFieldResolver.pickField(fields, headers.language()));
        appendNote(lines, "备注", FeishuFieldResolver.pickField(fields, headers.notes()));
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private static void appendNote(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + ": " + value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
