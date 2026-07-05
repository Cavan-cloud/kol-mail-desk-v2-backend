package com.lovart.maildesk.application.kol;

import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.KolDto;
import com.lovart.maildesk.application.dto.KolUpdateRequest;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class KolApplicationService {

    private final KolMapper kols;
    private final AuditLogService auditLog;

    public KolApplicationService(KolMapper kols, AuditLogService auditLog) {
        this.kols = kols;
        this.auditLog = auditLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public KolDto updateKol(UUID userId, UUID kolId, KolUpdateRequest request) {
        KolDO existing = kols.selectById(kolId);
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "达人不存在");
        }
        assertCanEdit(userId, existing);

        boolean hasName = request.name() != null && !request.name().isBlank();
        boolean hasStage = request.stage() != null;
        boolean hasReplyResolved = request.replyResolved() != null;
        if (!hasName && !hasStage && !hasReplyResolved) {
            throw new BusinessException("VALIDATION_ERROR", "至少需要更新一个字段");
        }

        KolDO patch = new KolDO();
        patch.setId(kolId);

        if (hasName) {
            String trimmedName = request.name().trim();
            patch.setName(trimmedName);
            patch.setNameOverridden(true);
            auditLog.append(
                    ActionType.STAGE_CHANGE,
                    "kol",
                    kolId,
                    Map.of(
                            "field", "name",
                            "from", existing.getName() == null ? "" : existing.getName(),
                            "to", trimmedName));
            existing.setName(trimmedName);
            existing.setNameOverridden(true);
        }

        if (hasStage) {
            KolStage nextStage = request.stage();
            patch.setStage(nextStage);
            patch.setStageOverride(true);
            auditLog.append(
                    ActionType.STAGE_CHANGE,
                    "kol",
                    kolId,
                    stageChangeMetadata(existing.getStage(), nextStage));
            existing.setStage(nextStage);
            existing.setStageOverride(true);
        }

        if (hasReplyResolved) {
            boolean nextResolved = request.replyResolved();
            patch.setReplyResolved(nextResolved);
            auditLog.append(
                    ActionType.STAGE_CHANGE,
                    "kol",
                    kolId,
                    Map.of(
                            "field", "reply_resolved",
                            "from", Boolean.TRUE.equals(existing.getReplyResolved()),
                            "to", nextResolved));
            existing.setReplyResolved(nextResolved);
        }

        kols.updateById(patch);
        return EntityMappers.toKolDto(existing);
    }

    private static Map<String, ?> stageChangeMetadata(KolStage from, KolStage to) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("from_stage", from == null ? null : from.name().toLowerCase());
        metadata.put("to_stage", to.name().toLowerCase());
        return metadata;
    }

    private static void assertCanEdit(UUID userId, KolDO kol) {
        UUID ownerUserId = kol.getOwnerUserId();
        if (ownerUserId != null && !ownerUserId.equals(userId)) {
            throw new BusinessException("FORBIDDEN", "无权编辑该达人");
        }
    }
}
