package com.lovart.maildesk.application.sync.feishu;

/**
 * Zero-based column indices resolved from a sheet header row.
 */
public record FeishuColumnIndex(
        Integer email,
        Integer operator,
        Integer name,
        Integer profileUrl,
        Integer platform,
        Integer country,
        Integer language,
        Integer type,
        Integer followers,
        Integer brandQuote,
        /** Secondary quote column (e.g. {@code KOL报价($)}) when {@code brandQuote} cell is blank. */
        Integer kolQuote,
        Integer finalCooperationPrice,
        Integer cooperation,
        Integer finalCooperation,
        Integer stage,
        Integer outreachDate,
        Integer notes) {

    public boolean hasEmailColumn() {
        return email != null;
    }

    public boolean hasOperatorColumn() {
        return operator != null;
    }
}
