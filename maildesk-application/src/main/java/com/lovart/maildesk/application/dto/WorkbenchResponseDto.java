package com.lovart.maildesk.application.dto;

import java.util.List;

public record WorkbenchResponseDto(
        List<WorkbenchKolDto> data,
        WorkbenchSidebarStatsDto sidebar,
        PageMetaDto page
) {
}
