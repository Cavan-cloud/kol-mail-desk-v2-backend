package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardFunnelStageDto;
import com.lovart.maildesk.application.dto.BoardKpiDto;
import com.lovart.maildesk.application.dto.BoardStageDistributionDto;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.application.support.BoardWindow;
import com.lovart.maildesk.application.support.StageCatalog;
import com.lovart.maildesk.common.enums.EmailDirection;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BoardApplicationService {

    private static final List<KolStage> COOPERATION_STAGES = List.of(
            KolStage.CONFIRMED,
            KolStage.PRODUCING,
            KolStage.REVIEWING,
            KolStage.PUBLISHED,
            KolStage.PAYING
    );

    private final KolMapper kols;
    private final EmailMapper emails;

    public BoardApplicationService(KolMapper kols, EmailMapper emails) {
        this.kols = kols;
        this.emails = emails;
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
        int unreplied = (int) scoped.stream().filter(BoardApplicationService::needsReply).count();
        int unreadEmails = countUnreadInboundEmails(scoped.stream().map(KolDO::getId).toList());
        int cooperation = COOPERATION_STAGES.stream()
                .mapToInt(stage -> snapshot.getOrDefault(stage, 0))
                .sum();

        int outreachCum = StageCatalog.cumulativeCount(snapshot, KolStage.OUTREACH);
        int payingCum = StageCatalog.cumulativeCount(snapshot, KolStage.PAYING);
        float conversion = outreachCum > 0 ? (float) payingCum / outreachCum : 0f;

        BoardKpiDto kpi = new BoardKpiDto(total, unreplied, unreadEmails, cooperation, conversion);

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

    private int countUnreadInboundEmails(List<UUID> kolIds) {
        if (kolIds.isEmpty()) {
            return 0;
        }
        Long count = emails.selectCount(
                new LambdaQueryWrapper<EmailDO>()
                        .in(EmailDO::getKolId, kolIds)
                        .eq(EmailDO::getDirection, EmailDirection.INBOUND)
                        .eq(EmailDO::getIsRead, false));
        return count == null ? 0 : count.intValue();
    }

    /**
     * Needs reply: latest activity is inbound and not manually resolved.
     */
    private static boolean needsReply(KolDO kol) {
        if (Boolean.TRUE.equals(kol.getReplyResolved())) {
            return false;
        }
        if (kol.getLastInboundAt() == null) {
            return false;
        }
        if (kol.getLastOutboundAt() != null && kol.getLastOutboundAt().isAfter(kol.getLastInboundAt())) {
            return false;
        }
        return true;
    }
}
