package com.lovart.maildesk.application.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.PageMetaDto;
import com.lovart.maildesk.application.dto.ScheduledEmailCreateRequest;
import com.lovart.maildesk.application.dto.ScheduledEmailDto;
import com.lovart.maildesk.application.dto.ScheduledEmailListResponseDto;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.common.enums.ScheduledEmailStatus;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.domain.scheduled.ScheduledEmailStateMachine;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import com.lovart.maildesk.domain.scheduled.mapper.ScheduledEmailMapper;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScheduledEmailApplicationService {

    private final ScheduledEmailMapper scheduledEmails;
    private final KolMapper kols;
    private final ProfileMapper profiles;
    private final EmailTemplateMapper templates;

    public ScheduledEmailApplicationService(
            ScheduledEmailMapper scheduledEmails,
            KolMapper kols,
            ProfileMapper profiles,
            EmailTemplateMapper templates
    ) {
        this.scheduledEmails = scheduledEmails;
        this.kols = kols;
        this.profiles = profiles;
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public ScheduledEmailListResponseDto listForUser(UUID userId) {
        List<ScheduledEmailDO> rows = scheduledEmails.selectList(
                new LambdaQueryWrapper<ScheduledEmailDO>()
                        .eq(ScheduledEmailDO::getUserId, userId)
                        .orderByDesc(ScheduledEmailDO::getScheduledAt));

        Map<UUID, String> kolNames = loadKolNames(rows);
        List<ScheduledEmailDto> data = rows.stream()
                .map(row -> EntityMappers.toScheduledEmailDto(
                        row,
                        row.getKolId() == null ? null : kolNames.get(row.getKolId())
                ))
                .toList();
        PageMetaDto page = new PageMetaDto(1, data.size(), data.size());
        return new ScheduledEmailListResponseDto(data, page);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScheduledEmailDto create(UUID userId, ScheduledEmailCreateRequest request) {
        assertActiveUser(userId);
        if (!request.scheduledAt().isAfter(OffsetDateTime.now())) {
            throw new BusinessException("VALIDATION_ERROR", "定时发送时间必须晚于当前时间");
        }

        KolDO kol = kols.selectById(request.kolId());
        if (kol == null) {
            throw new BusinessException("NOT_FOUND", "达人不存在");
        }

        if (request.templateId() != null) {
            EmailTemplateDO template = templates.selectById(request.templateId());
            if (template == null || !userId.equals(template.getCreatedBy())) {
                throw new BusinessException("FORBIDDEN", "无权使用该模板");
            }
        }

        ScheduledEmailDO row = new ScheduledEmailDO();
        row.setKolId(request.kolId());
        row.setUserId(userId);
        row.setTemplateId(request.templateId());
        row.setToEmail(request.to().trim());
        row.setCcEmails(request.ccEmails() == null ? List.of() : request.ccEmails());
        row.setSubject(request.subject().trim());
        row.setEnglishBody(request.englishBody());
        row.setEnglishBodyHtml(request.englishBodyHtml());
        row.setChineseDraft(request.chineseDraft());
        row.setScheduledAt(request.scheduledAt());
        row.setStatus(ScheduledEmailStatus.SCHEDULED.dbValue());
        row.setAttemptCount(0);
        scheduledEmails.insert(row);

        String kolName = kol.getName() != null ? kol.getName() : kol.getEmail();
        return EntityMappers.toScheduledEmailDto(row, kolName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(UUID userId, UUID scheduledEmailId) {
        assertActiveUser(userId);
        ScheduledEmailDO existing = scheduledEmails.selectById(scheduledEmailId);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BusinessException("NOT_FOUND", "定时邮件不存在");
        }
        if (!ScheduledEmailStateMachine.canCancel(ScheduledEmailStatus.fromDbValue(existing.getStatus()))) {
            throw new BusinessException("CONFLICT", "已发送或正在发送，无法取消");
        }

        ScheduledEmailDO patch = new ScheduledEmailDO();
        patch.setId(scheduledEmailId);
        patch.setStatus(ScheduledEmailStatus.CANCELLED.dbValue());
        patch.setCancelledAt(OffsetDateTime.now());
        scheduledEmails.updateById(patch);
    }

    private void assertActiveUser(UUID userId) {
        ProfileDO profile = profiles.selectById(userId);
        if (profile == null || UserStatus.DEPARTED.dbValue().equals(profile.getStatus())) {
            throw new BusinessException("FORBIDDEN", "已离职账号无法操作");
        }
    }

    private Map<UUID, String> loadKolNames(List<ScheduledEmailDO> rows) {
        Map<UUID, String> names = new HashMap<>();
        for (ScheduledEmailDO row : rows) {
            UUID kolId = row.getKolId();
            if (kolId == null || names.containsKey(kolId)) {
                continue;
            }
            KolDO kol = kols.selectById(kolId);
            if (kol != null) {
                names.put(kolId, kol.getName() != null ? kol.getName() : kol.getEmail());
            }
        }
        return names;
    }
}
