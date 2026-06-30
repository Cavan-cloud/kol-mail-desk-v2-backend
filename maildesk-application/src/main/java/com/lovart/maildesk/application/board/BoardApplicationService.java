package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardFunnelStageDto;
import com.lovart.maildesk.application.dto.BoardKpiDto;
import com.lovart.maildesk.application.dto.BoardStageDistributionDto;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.application.support.BoardWindow;
import com.lovart.maildesk.application.support.StageCatalog;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BoardApplicationService {

    private final KolMapper kols;

    public BoardApplicationService(KolMapper kols) {
        this.kols = kols;
    }

    @Transactional(readOnly = true)
    public BoardSummaryDto getBoard(String windowParam) {
        BoardWindow window = BoardWindow.parse(windowParam);
        List<KolDO> rows = kols.selectList(new LambdaQueryWrapper<KolDO>());
        List<KolDO> scoped = rows.stream()
                .filter(k -> window.matches(k.getFeishuOutreachAt()))
                .toList();

        Map<KolStage, Integer> snapshot = new EnumMap<>(KolStage.class);
        for (KolStage stage : StageCatalog.ALL_STAGES) {
            snapshot.put(stage, 0);
        }
        for (KolDO kol : scoped) {
            if (kol.getStage() != null) {
                snapshot.merge(kol.getStage(), 1, Integer::sum);
            }
        }

        int total = scoped.size();
        int active = (int) scoped.stream()
                .filter(k -> !"declined".equals(k.getStatus()) && !"closed".equals(k.getStatus()))
                .count();
        int published = countStageAtLeast(snapshot, KolStage.PUBLISHED);
        int paid = snapshot.getOrDefault(KolStage.PAYING, 0);
        float conversion = total > 0 ? (float) paid / total : 0f;

        BoardKpiDto kpi = new BoardKpiDto(total, active, published, paid, conversion);

        List<BoardFunnelStageDto> funnel = StageCatalog.FUNNEL_STAGES.stream()
                .map(stage -> new BoardFunnelStageDto(
                        StageCatalog.jsonStage(stage),
                        StageCatalog.label(stage),
                        StageCatalog.cumulativeCount(snapshot, stage)
                ))
                .toList();

        List<BoardStageDistributionDto> distribution = StageCatalog.ALL_STAGES.stream()
                .map(stage -> new BoardStageDistributionDto(
                        StageCatalog.jsonStage(stage),
                        StageCatalog.label(stage),
                        snapshot.getOrDefault(stage, 0)
                ))
                .toList();

        return new BoardSummaryDto(window.raw(), kpi, funnel, distribution);
    }

    private static int countStageAtLeast(Map<KolStage, Integer> snapshot, KolStage threshold) {
        int idx = StageCatalog.FUNNEL_STAGES.indexOf(threshold);
        if (idx < 0) {
            return snapshot.getOrDefault(threshold, 0);
        }
        int sum = 0;
        for (int i = idx; i < StageCatalog.FUNNEL_STAGES.size(); i++) {
            sum += snapshot.getOrDefault(StageCatalog.FUNNEL_STAGES.get(i), 0);
        }
        return sum;
    }
}
