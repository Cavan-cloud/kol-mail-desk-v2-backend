package com.lovart.maildesk.application.sync.feishu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.common.feishu.FeishuTabFilterMode;
import com.lovart.maildesk.domain.feishu.FeishuBitableRecord;
import com.lovart.maildesk.domain.feishu.FeishuBitableTableMeta;
import com.lovart.maildesk.domain.feishu.FeishuClient;
import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.integration.feishu.FeishuProperties;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates read-only Feishu fetch (Bitable or legacy Sheet), row parsing, and KOL upsert.
 */
@Service
public class FeishuSyncService {

    private static final int UPSERT_CHUNK_SIZE = 200;
    private static final String SKIP_RECENT_WINDOW = "out of recent-months window";
    private static final String SKIP_NOT_ROSTER_TAB = "not a roster tab (month or regional)";

    private final FeishuClient feishuClient;
    private final FeishuProperties feishuProperties;
    private final ProfileMapper profiles;
    private final FeishuKolUpsertService upsertService;

    public FeishuSyncService(
            FeishuClient feishuClient,
            FeishuProperties feishuProperties,
            ProfileMapper profiles,
            FeishuKolUpsertService upsertService) {
        this.feishuClient = feishuClient;
        this.feishuProperties = feishuProperties;
        this.profiles = profiles;
        this.upsertService = upsertService;
    }

    public FeishuSyncResult sync() {
        return sync(null);
    }

    public FeishuSyncResult sync(FeishuSyncOptions options) {
        FeishuSyncOptions opts = resolveOptions(options);
        if (!feishuClient.isConfigured()) {
            return FeishuSyncResult.notConfigured();
        }
        if (feishuClient.isBitableSource()) {
            return syncBitable(opts);
        }
        return syncSheet(opts);
    }

    FeishuSyncOptions resolveOptions(FeishuSyncOptions options) {
        FeishuSyncOptions base = options == null ? FeishuSyncOptions.defaults() : options;
        Integer maxRecords = feishuProperties.isFullSync() ? null : base.maxRecords();
        int recentMonths = base.recentMonths();
        List<String> regionalTabs = feishuProperties.resolvedRegionalTabNames();
        return new FeishuSyncOptions(
                base.sheetId(),
                base.tableId(),
                base.dryRun(),
                maxRecords,
                recentMonths,
                regionalTabs);
    }

    private FeishuSyncResult syncBitable(FeishuSyncOptions opts) {
        String appToken = feishuClient.configuredKolAppToken();
        List<FeishuBitableTableMeta> allTables = feishuClient.listBitableTables();
        Set<String> regionalTabs = feishuProperties.resolvedRegionalTabNameSet();
        List<FeishuBitableTableMeta> syncTables = filterBitableTables(allTables, regionalTabs);
        Set<String> syncTableIds = syncTables.stream()
                .map(FeishuBitableTableMeta::tableId)
                .collect(Collectors.toSet());

        List<FeishuBitableTableMeta> tablesToScan;
        if (opts.tableId() != null && !opts.tableId().isBlank()) {
            tablesToScan = allTables.stream()
                    .filter(table -> table.tableId().equals(opts.tableId()))
                    .toList();
        } else {
            tablesToScan = syncTables;
        }

        List<FeishuSyncResult.SkippedSheet> skippedTables = new ArrayList<>();
        if (opts.tableId() == null || opts.tableId().isBlank()) {
            for (FeishuBitableTableMeta table : allTables) {
                if (!syncTableIds.contains(table.tableId())) {
                    skippedTables.add(new FeishuSyncResult.SkippedSheet(table.name(), skipReasonForFilter()));
                }
            }
        }

        FeishuFieldHeaders headers = FeishuFieldHeaders.defaults();
        Map<String, FeishuKolDraft> merged = new LinkedHashMap<>();
        int scannedRows = 0;

        for (FeishuBitableTableMeta table : tablesToScan) {
            List<FeishuBitableRecord> records =
                    feishuClient.listBitableRecords(appToken, table.tableId(), opts.maxRecords());
            if (records.isEmpty()) {
                skippedTables.add(new FeishuSyncResult.SkippedSheet(table.name(), "empty"));
                continue;
            }

            boolean allowNoOperator = opts.noOperatorSheets().contains(table.name());
            if (!hasEmailField(records.getFirst().fields(), headers)) {
                skippedTables.add(new FeishuSyncResult.SkippedSheet(table.name(), "missing email header"));
                continue;
            }
            if (!allowNoOperator && !hasOperatorField(records.getFirst().fields(), headers)) {
                skippedTables.add(new FeishuSyncResult.SkippedSheet(table.name(), "missing operator header"));
                continue;
            }

            for (FeishuBitableRecord record : records) {
                scannedRows++;
                FeishuBitableRowMapper.mapRecord(record, headers, table.name()).ifPresent(draft -> {
                    String key = FeishuCellExtractor.mergeKey(draft.email(), draft.operatorName());
                    merged.merge(key, draft, FeishuKolDraft::preferRicherPrices);
                });

                if (opts.maxRecords() != null && merged.size() >= opts.maxRecords()) {
                    break;
                }
            }
            if (opts.maxRecords() != null && merged.size() >= opts.maxRecords()) {
                break;
            }
        }

        return finalizeSync(appToken, tablesToScan.size(), skippedTables, scannedRows, merged, opts);
    }

    private FeishuSyncResult syncSheet(FeishuSyncOptions opts) {
        String spreadsheetToken = feishuClient.configuredKolAppToken();
        List<FeishuSheetMeta> allSheets = feishuClient.listSheets();
        Set<String> regionalTabs = feishuProperties.resolvedRegionalTabNameSet();
        List<FeishuSheetMeta> filteredSheets = FeishuSheetFilter.filterSheets(
                allSheets,
                feishuProperties.tabFilterMode(),
                opts.recentMonths(),
                regionalTabs);
        List<FeishuSheetMeta> sheetsToScan = opts.sheetId() != null
                ? allSheets.stream().filter(s -> s.sheetId().equals(opts.sheetId())).toList()
                : filteredSheets;

        List<FeishuSyncResult.SkippedSheet> skippedSheets = new ArrayList<>();
        if (opts.sheetId() == null) {
            for (FeishuSheetMeta sheet : allSheets) {
                if (!sheetsToScan.contains(sheet)) {
                    skippedSheets.add(new FeishuSyncResult.SkippedSheet(sheet.title(), skipReasonForFilter()));
                }
            }
        }

        FeishuFieldHeaders headers = FeishuFieldHeaders.defaults();
        Map<String, FeishuKolDraft> merged = new LinkedHashMap<>();
        int scannedRows = 0;

        for (FeishuSheetMeta sheet : sheetsToScan) {
            List<List<Object>> rows = readSheetRows(sheet, opts.maxRecords());
            if (rows.isEmpty()) {
                skippedSheets.add(new FeishuSyncResult.SkippedSheet(sheet.title(), "empty"));
                continue;
            }

            List<String> headerRow = FeishuColumnResolver.normalizeHeaderRow(rows.getFirst());
            FeishuColumnIndex columns = FeishuColumnResolver.resolve(headerRow, headers);
            boolean allowNoOperator = opts.noOperatorSheets().contains(sheet.title());

            if (!columns.hasEmailColumn()) {
                skippedSheets.add(new FeishuSyncResult.SkippedSheet(sheet.title(), "missing email header"));
                continue;
            }
            if (!columns.hasOperatorColumn() && !allowNoOperator) {
                skippedSheets.add(new FeishuSyncResult.SkippedSheet(sheet.title(), "missing operator header"));
                continue;
            }

            for (int i = 1; i < rows.size(); i++) {
                List<?> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                scannedRows++;

                FeishuRowMapper.mapRow(row, columns, sheet.title()).ifPresent(draft -> {
                    String key = FeishuCellExtractor.mergeKey(draft.email(), draft.operatorName());
                    merged.merge(key, draft, FeishuKolDraft::preferRicherPrices);
                });

                if (opts.maxRecords() != null && merged.size() >= opts.maxRecords()) {
                    break;
                }
            }
            if (opts.maxRecords() != null && merged.size() >= opts.maxRecords()) {
                break;
            }
        }

        return finalizeSync(spreadsheetToken, sheetsToScan.size(), skippedSheets, scannedRows, merged, opts);
    }

    private List<FeishuBitableTableMeta> filterBitableTables(
            List<FeishuBitableTableMeta> allTables, Set<String> regionalTabs) {
        return switch (feishuProperties.tabFilterMode()) {
            case ALL -> allTables;
            case RECENT_MONTHS, ROSTER -> FeishuBitableTableFilter.filterSyncTables(allTables, regionalTabs);
        };
    }

    private String skipReasonForFilter() {
        return feishuProperties.tabFilterMode() == FeishuTabFilterMode.RECENT_MONTHS
                ? SKIP_RECENT_WINDOW
                : SKIP_NOT_ROSTER_TAB;
    }

    private FeishuSyncResult finalizeSync(
            String feishuSourceToken,
            int tablesScanned,
            List<FeishuSyncResult.SkippedSheet> skipped,
            int scannedRows,
            Map<String, FeishuKolDraft> merged,
            FeishuSyncOptions opts) {
        List<FeishuKolDraft> drafts = List.copyOf(merged.values());
        Map<String, UUID> ownersByOperator = loadOwnersByOperatorName();
        List<FeishuSyncResult.SampleRow> samples = buildSamples(drafts, ownersByOperator);

        if (opts.dryRun()) {
            return new FeishuSyncResult(
                    "synced",
                    null,
                    tablesScanned,
                    List.copyOf(skipped),
                    scannedRows,
                    merged.size(),
                    drafts.size(),
                    0,
                    upsertService.countOwnerMatches(drafts, ownersByOperator),
                    true,
                    samples);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int upserted = 0;
        int skippedRows = 0;
        int ownerMatched = 0;
        for (int i = 0; i < drafts.size(); i += UPSERT_CHUNK_SIZE) {
            List<FeishuKolDraft> chunk = drafts.subList(i, Math.min(i + UPSERT_CHUNK_SIZE, drafts.size()));
            FeishuKolUpsertService.UpsertStats stats =
                    upsertService.upsertChunkTransactional(chunk, feishuSourceToken, ownersByOperator, now);
            upserted += stats.upserted();
            skippedRows += stats.skipped();
            ownerMatched += stats.ownerMatched();
        }

        return new FeishuSyncResult(
                "synced",
                null,
                tablesScanned,
                List.copyOf(skipped),
                scannedRows,
                merged.size(),
                upserted,
                skippedRows,
                ownerMatched,
                false,
                samples);
    }

    static boolean hasEmailField(Map<String, Object> fields, FeishuFieldHeaders headers) {
        return hasField(fields, headers.email());
    }

    static boolean hasOperatorField(Map<String, Object> fields, FeishuFieldHeaders headers) {
        return hasField(fields, headers.operator());
    }

    private static boolean hasField(Map<String, Object> fields, List<String> candidates) {
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (fields.containsKey(candidate)) {
                return true;
            }
            for (String key : fields.keySet()) {
                if (key != null && key.contains(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<List<Object>> readSheetRows(FeishuSheetMeta sheet, Integer maxRecords) {
        if (maxRecords != null) {
            return feishuClient.readSheetValues(feishuClient.configuredKolAppToken(), sheet, maxRecords);
        }
        return feishuClient.readSheetValues(sheet);
    }

    private Map<String, UUID> loadOwnersByOperatorName() {
        List<ProfileDO> rows = profiles.selectList(new LambdaQueryWrapper<ProfileDO>()
                .isNotNull(ProfileDO::getFeishuOperatorName)
                .ne(ProfileDO::getFeishuOperatorName, ""));
        Map<String, UUID> owners = new LinkedHashMap<>();
        for (ProfileDO profile : rows) {
            String key = FeishuCellExtractor.normalizeOperatorName(profile.getFeishuOperatorName());
            if (!key.isEmpty()) {
                owners.putIfAbsent(key, profile.getId());
            }
        }
        return owners;
    }

    private static List<FeishuSyncResult.SampleRow> buildSamples(
            List<FeishuKolDraft> drafts, Map<String, UUID> ownersByOperator) {
        List<FeishuSyncResult.SampleRow> samples = new ArrayList<>();
        for (FeishuKolDraft draft : drafts) {
            if (samples.size() >= 8) {
                break;
            }
            UUID owner = ownersByOperator.get(FeishuCellExtractor.normalizeOperatorName(draft.operatorName()));
            samples.add(new FeishuSyncResult.SampleRow(
                    draft.email(),
                    draft.operatorName(),
                    owner != null,
                    draft.name()));
        }
        return samples;
    }
}
