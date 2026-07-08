package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceDetailTest {

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private ProfileMapper profiles;

    private BoardApplicationService service;

    @BeforeEach
    void setUp() {
        service = new BoardApplicationService(kols, emails, profiles);
    }

    @Test
    void getBoard_unrepliedDetail_matchesKpiCount() {
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        KolDO unrepliedKol = unrepliedKol();
        KolDO repliedKol = repliedKol();
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(unrepliedKol, repliedKol));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        BoardSummaryDto result = service.getBoard("all", null, true, "unreplied", null, null, null);

        assertThat(result.kpi().unrepliedKols()).isEqualTo(1);
        assertThat(result.kols()).hasSize(1);
        assertThat(result.kols().getFirst().unreplied()).isTrue();
    }

    private static KolDO unrepliedKol() {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("a@example.com");
        kol.setName("A");
        kol.setStage(KolStage.OUTREACH);
        kol.setLastInboundAt(OffsetDateTime.now(ZoneOffset.UTC));
        kol.setReplyResolved(false);
        return kol;
    }

    private static KolDO repliedKol() {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("b@example.com");
        kol.setName("B");
        kol.setStage(KolStage.REPLIED);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        kol.setLastInboundAt(now.minusDays(1));
        kol.setLastOutboundAt(now);
        kol.setReplyResolved(false);
        return kol;
    }
}
