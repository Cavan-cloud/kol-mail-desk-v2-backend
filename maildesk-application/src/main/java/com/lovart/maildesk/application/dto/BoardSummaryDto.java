package com.lovart.maildesk.application.dto;

import java.util.List;

public record BoardSummaryDto(
        String window,
        BoardKpiDto kpi,
        List<BoardFunnelStageDto> funnel,
        List<BoardStageDistributionDto> stageDistribution
) {
}
