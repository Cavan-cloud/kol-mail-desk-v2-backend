package com.lovart.maildesk.application.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.EmailTemplateDto;
import com.lovart.maildesk.application.dto.PageMetaDto;
import com.lovart.maildesk.application.dto.TemplateListResponseDto;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.domain.template.entity.EmailTemplateDO;
import com.lovart.maildesk.domain.template.mapper.EmailTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateApplicationService {

    private final EmailTemplateMapper templates;

    public TemplateApplicationService(EmailTemplateMapper templates) {
        this.templates = templates;
    }

    @Transactional(readOnly = true)
    public TemplateListResponseDto listTemplates() {
        List<EmailTemplateDO> rows = templates.selectList(
                new LambdaQueryWrapper<EmailTemplateDO>()
                        .orderByDesc(EmailTemplateDO::getCreatedAt));
        List<EmailTemplateDto> data = rows.stream().map(EntityMappers::toEmailTemplateDto).toList();
        PageMetaDto page = new PageMetaDto(1, data.size(), data.size());
        return new TemplateListResponseDto(data, page);
    }
}
