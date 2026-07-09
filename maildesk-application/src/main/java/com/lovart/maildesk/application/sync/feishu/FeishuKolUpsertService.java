package com.lovart.maildesk.application.sync.feishu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists parsed Feishu rows with composite-key upsert semantics.
 */
@Service
public class FeishuKolUpsertService {

    private static final String SOURCE_FEISHU = "feishu";
    private static final String SOURCE_MANUAL = "manual";

    private final KolMapper kols;

    public FeishuKolUpsertService(KolMapper kols) {
        this.kols = kols;
    }

    public UpsertStats upsertChunk(
            List<FeishuKolDraft> chunk,
            String spreadsheetToken,
            Map<String, UUID> ownersByOperator,
            OffsetDateTime now) {
        int upserted = 0;
        int skipped = 0;
        int ownerMatched = 0;

        for (FeishuKolDraft draft : chunk) {
            RowUpsertResult result = upsertOne(draft, spreadsheetToken, ownersByOperator, now);
            if (result.outcome() == UpsertOutcome.SKIPPED) {
                skipped++;
            } else {
                upserted++;
                if (result.ownerMatched()) {
                    ownerMatched++;
                }
            }
        }
        return new UpsertStats(upserted, skipped, ownerMatched);
    }

    @Transactional(rollbackFor = Exception.class)
    public UpsertStats upsertChunkTransactional(
            List<FeishuKolDraft> chunk,
            String spreadsheetToken,
            Map<String, UUID> ownersByOperator,
            OffsetDateTime now) {
        return upsertChunk(chunk, spreadsheetToken, ownersByOperator, now);
    }

    public int countOwnerMatches(List<FeishuKolDraft> drafts, Map<String, UUID> ownersByOperator) {
        int matched = 0;
        for (FeishuKolDraft draft : drafts) {
            if (resolveOwner(null, draft.operatorName(), ownersByOperator) != null) {
                matched++;
            }
        }
        return matched;
    }

    private RowUpsertResult upsertOne(
            FeishuKolDraft draft,
            String spreadsheetToken,
            Map<String, UUID> ownersByOperator,
            OffsetDateTime now) {
        String operatorName = normalizeOperator(draft.operatorName());
        KolDO existing = findExisting(draft.email(), operatorName);

        if (existing != null && SOURCE_MANUAL.equals(existing.getSource())) {
            return new RowUpsertResult(UpsertOutcome.SKIPPED, false);
        }

        UUID ownerUserId = resolveOwner(existing, draft.operatorName(), ownersByOperator);
        boolean ownerMatched = ownerUserId != null
                && (existing == null || existing.getOwnerUserId() == null);

        if (existing == null) {
            KolDO insert = new KolDO();
            applyFeishuFields(insert, draft, operatorName, spreadsheetToken, now, ownerUserId, false, false);
            kols.insert(insert);
            return new RowUpsertResult(UpsertOutcome.INSERTED, ownerMatched);
        }

        KolDO patch = new KolDO();
        patch.setId(existing.getId());
        applyFeishuFields(patch, draft, operatorName, spreadsheetToken, now,
                existing.getOwnerUserId() == null ? ownerUserId : null,
                Boolean.TRUE.equals(existing.getNameOverridden()),
                Boolean.TRUE.equals(existing.getStageOverride()));
        kols.updateById(patch);
        return new RowUpsertResult(UpsertOutcome.UPDATED, ownerMatched);
    }

    private static void applyFeishuFields(
            KolDO row,
            FeishuKolDraft draft,
            String operatorName,
            String spreadsheetToken,
            OffsetDateTime now,
            UUID ownerUserId,
            boolean preserveName,
            boolean preserveStage) {
        row.setEmail(draft.email());
        row.setFeishuOperatorName(operatorName);
        if (!preserveName) {
            row.setName(draft.displayName());
        }
        row.setHandle(draft.handle());
        row.setPlatformHandle(draft.handle());
        row.setPrimaryPlatform(Platform.fromDbValue(draft.primaryPlatform()));
        row.setType(draft.type());
        row.setExternalProfileUrl(draft.profileUrl());
        row.setBrandQuote(draft.brandQuote());
        row.setFinalCooperationPrice(draft.finalCooperationPrice());
        row.setAgreedPrice(draft.agreedPrice());
        row.setNotes(draft.notes());
        row.setSource(SOURCE_FEISHU);
        row.setFeishuTableId(spreadsheetToken);
        row.setLastFeishuSyncedAt(now);
        row.setFeishuOutreachAt(draft.feishuOutreachAt());
        row.setStatus(KolStatus.ACTIVE);
        if (!preserveStage && draft.stage() != null) {
            row.setStage(draft.stage());
        }
        if (ownerUserId != null) {
            row.setOwnerUserId(ownerUserId);
        }
    }

    private KolDO findExisting(String email, String operatorName) {
        return kols.selectOne(new LambdaQueryWrapper<KolDO>()
                .eq(KolDO::getEmail, email)
                .eq(KolDO::getFeishuOperatorName, operatorName)
                .last("LIMIT 1"));
    }

    private static UUID resolveOwner(KolDO existing, String operatorName, Map<String, UUID> ownersByOperator) {
        if (existing != null && existing.getOwnerUserId() != null) {
            return existing.getOwnerUserId();
        }
        if (operatorName == null || operatorName.isBlank()) {
            return null;
        }
        return ownersByOperator.get(FeishuCellExtractor.normalizeOperatorName(operatorName));
    }

    private static String normalizeOperator(String operatorName) {
        return operatorName == null ? "" : operatorName.trim();
    }

    public record UpsertStats(int upserted, int skipped, int ownerMatched) {
    }

    private enum UpsertOutcome {
        INSERTED, UPDATED, SKIPPED
    }

    private record RowUpsertResult(UpsertOutcome outcome, boolean ownerMatched) {
    }
}
