package com.lovart.maildesk.application.sync.feishu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.domain.feishu.FeishuClient;
import com.lovart.maildesk.domain.feishu.FeishuSheetMeta;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates read-only Feishu sheet fetch, row parsing, and KOL upsert.
 * Ported from legacy {@code syncFeishuKols()} in {@code lib/feishu/sync-kols.ts}.
 */
@Service
public class FeishuSyncService {

    private static final int UPSERT_CHUNK_SIZE = 200;
    private static final String SKIP_RECENT_WINDOW = "out of recent-months window";

    private final FeishuClient feishuClient;
    private final ProfileMapper profiles;
    private final FeishuKolUpsertService upsertService;

    public FeishuSyncService(
            FeishuClient feishuClient,
            ProfileMapper profiles,
            FeishuKolUpsertService upsertService) {
        this.feishuClient = feishuClient;
        this.profiles = profiles;
        this.upsertService = upsertService;
    }

    public FeishuSyncResult sync() {
        return sync(FeishuSyncOptions.defaults());
    }

    public FeishuSyncResult sync(FeishuSyncOptions options) {
        FeishuSyncOptions opts = options == null ? FeishuSyncOptions.defaults() : options;
        if (!feishuClient.isConfigured()) {
            return FeishuSyncResult.notConfigured();
        }

        String spreadsheetToken = feishuClient.configuredKolAppToken();
        List<FeishuSheetMeta> allSheets = feishuClient.listSheets();
        List<FeishuSheetMeta> filteredSheets =
                FeishuSheetFilter.filterRecentMonthSheets(allSheets, opts.recentMonths());
        List<FeishuSheetMeta> sheetsToScan = opts.sheetId() != null
                ? allSheets.stream().filter(s -> s.sheetId().equals(opts.sheetId())).toList()
                : filteredSheets;

        List<FeishuSyncResult.SkippedSheet> skippedSheets = new ArrayList<>();
        if (opts.sheetId() == null) {
            for (FeishuSheetMeta sheet : allSheets) {
                if (!sheetsToScan.contains(sheet)) {
                    skippedSheets.add(new FeishuSyncResult.SkippedSheet(sheet.title(), SKIP_RECENT_WINDOW));
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
                    merged.putIfAbsent(key, draft);
                });

                if (opts.maxRecords() != null && merged.size() >= opts.maxRecords()) {
                    break;
                }
            }
            if (opts.maxRecords() != null && merged.size() >= opts.maxRecords()) {
                break;
            }
        }

        List<FeishuKolDraft> drafts = List.copyOf(merged.values());
        Map<String, UUID> ownersByOperator = loadOwnersByOperatorName();
        List<FeishuSyncResult.SampleRow> samples = buildSamples(drafts, ownersByOperator);

        if (opts.dryRun()) {
            return new FeishuSyncResult(
                    "synced",
                    null,
                    sheetsToScan.size(),
                    List.copyOf(skippedSheets),
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
        int skipped = 0;
        int ownerMatched = 0;
        for (int i = 0; i < drafts.size(); i += UPSERT_CHUNK_SIZE) {
            List<FeishuKolDraft> chunk = drafts.subList(i, Math.min(i + UPSERT_CHUNK_SIZE, drafts.size()));
            FeishuKolUpsertService.UpsertStats stats =
                    upsertService.upsertChunkTransactional(chunk, spreadsheetToken, ownersByOperator, now);
            upserted += stats.upserted();
            skipped += stats.skipped();
            ownerMatched += stats.ownerMatched();
        }

        return new FeishuSyncResult(
                "synced",
                null,
                sheetsToScan.size(),
                List.copyOf(skippedSheets),
                scannedRows,
                merged.size(),
                upserted,
                skipped,
                ownerMatched,
                false,
                samples);
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
