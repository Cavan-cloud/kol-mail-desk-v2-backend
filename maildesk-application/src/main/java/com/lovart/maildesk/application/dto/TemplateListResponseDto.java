package com.lovart.maildesk.application.dto;

import java.util.List;

public record TemplateListResponseDto(
        List<EmailTemplateDto> data,
        PageMetaDto page
) {
}
