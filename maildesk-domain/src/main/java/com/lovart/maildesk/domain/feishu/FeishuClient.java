package com.lovart.maildesk.domain.feishu;

import java.util.List;

/**
 * Read-only Feishu Open API adapter (Sheet + Bitable). Write APIs are forbidden.
 * <p>
 * Implemented in {@code maildesk-integration}; consumed by {@code FeishuSyncService}
 * in application layer (P2-T03+).
 */
public interface FeishuClient {

    /** Whether {@code appId}, {@code appSecret}, and {@code kolAppToken} are all set. */
    boolean isConfigured();

    /**
     * Lightweight connectivity check: obtains a tenant token and lists data sources.
     * Does not mutate Feishu data.
     */
    FeishuConfigCheckResult verifyConfiguration();

    /** Whether sync reads from Bitable tables ({@code true}) or spreadsheet tabs ({@code false}). */
    boolean isBitableSource();

    /** Lists tabs in the configured KOL spreadsheet ({@code FEISHU_KOL_APP_TOKEN}). */
    List<FeishuSheetMeta> listSheets();

    /** Lists tabs in the given spreadsheet token. */
    List<FeishuSheetMeta> listSheets(String spreadsheetToken);

    /** Reads all cell values from a sheet tab (batched range requests). */
    List<List<Object>> readSheetValues(FeishuSheetMeta sheet);

    /**
     * Reads cell values from a sheet tab. When {@code maxRows} is set, caps data rows
     * (excluding header) for dry-run / delta sync.
     */
    List<List<Object>> readSheetValues(String spreadsheetToken, FeishuSheetMeta sheet, Integer maxRows);

    /** Lists data tables inside a Bitable app. */
    List<FeishuBitableTableMeta> listBitableTables(String appToken);

    /** Lists data tables in the configured Bitable app ({@code FEISHU_KOL_APP_TOKEN}). */
    List<FeishuBitableTableMeta> listBitableTables();

    /** Lists records from a Bitable table (paginated). */
    List<FeishuBitableRecord> listBitableRecords(String appToken, String tableId, Integer maxRecords);

    /** Lists records from the configured app token + table id. */
    List<FeishuBitableRecord> listBitableRecords(Integer maxRecords);

    /** Spreadsheet / Bitable app token used as {@code kols.feishu_table_id} on sync. */
    String configuredKolAppToken();
}
