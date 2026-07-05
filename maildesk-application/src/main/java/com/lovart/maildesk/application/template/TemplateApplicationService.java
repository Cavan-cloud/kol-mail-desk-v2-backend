package com.lovart.maildesk.application.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.EmailTemplateDto;
import com.lovart.maildesk.application.dto.PageMetaDto;
import com.lovart.maildesk.application.dto.TemplateListResponseDto;
import com.lovart.maildesk.application.dto.TemplateUpsertRequest;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TemplateApplicationService {

    private final EmailTemplateMapper templates;

    public TemplateApplicationService(EmailTemplateMapper templates) {
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public TemplateListResponseDto listTemplates(UUID userId) {
        List<EmailTemplateDO> rows = templates.selectList(
                new LambdaQueryWrapper<EmailTemplateDO>()
                        .eq(EmailTemplateDO::getCreatedBy, userId)
                        .orderByDesc(EmailTemplateDO::getCreatedAt));
        List<EmailTemplateDto> data = rows.stream().map(EntityMappers::toEmailTemplateDto).toList();
        PageMetaDto page = new PageMetaDto(1, data.size(), data.size());
        return new TemplateListResponseDto(data, page);
    }

    @Transactional(rollbackFor = Exception.class)
    public EmailTemplateDto createTemplate(UUID userId, TemplateUpsertRequest request) {
        EmailTemplateDO row = new EmailTemplateDO();
        applyUpsert(row, request);
        row.setUsedCount(0);
        templates.insert(row);
        return EntityMappers.toEmailTemplateDto(templates.selectById(row.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public EmailTemplateDto updateTemplate(UUID userId, UUID templateId, TemplateUpsertRequest request) {
        EmailTemplateDO existing = requireOwnedTemplate(userId, templateId);
        EmailTemplateDO patch = new EmailTemplateDO();
        patch.setId(existing.getId());
        applyUpsert(patch, request);
        templates.updateById(patch);
        return EntityMappers.toEmailTemplateDto(templates.selectById(templateId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(UUID userId, UUID templateId) {
        requireOwnedTemplate(userId, templateId);
        templates.deleteById(templateId);
    }

    private EmailTemplateDO requireOwnedTemplate(UUID userId, UUID templateId) {
        EmailTemplateDO existing = templates.selectById(templateId);
        if (existing == null) {
            throw new BusinessException("NOT_FOUND", "模板不存在或已删除");
        }
        if (existing.getCreatedBy() == null || !existing.getCreatedBy().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "无权操作此模板");
        }
        return existing;
    }

    private static void applyUpsert(EmailTemplateDO row, TemplateUpsertRequest request) {
        row.setName(request.name().trim());
        row.setScenario(request.scenario().trim());
        row.setSubject(request.subject().trim());
        row.setBody(request.body());
    }
}
