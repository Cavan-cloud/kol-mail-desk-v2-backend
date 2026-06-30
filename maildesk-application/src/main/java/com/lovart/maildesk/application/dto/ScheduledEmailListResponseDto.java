package com.lovart.maildesk.application.dto;

import java.util.List;

public record ScheduledEmailListResponseDto(
        List<ScheduledEmailDto> data,
        PageMetaDto page
) {
}
