package com.lovart.maildesk.application.dto;

import java.util.List;
import java.util.UUID;

public record BoardSummaryDto(
        String window,
        UUID selectedOwnerId,
        boolean includeInterns,
        BoardKpiDto kpi,
        List<BoardFunnelStageDto> funnel,
        List<BoardStageDistributionDto> stageDistribution,
        List<BoardKolDto> kols,
        List<BoardMemberRowDto> members,
        List<BoardPlatformSegmentDto> platformDistribution,
        List<BoardKolDto> recentActivity,
        List<String> availableMonths,
        PageMetaDto kolsPage
) {
}
