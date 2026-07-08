package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.common.enums.KolStage;
import com.lovart.maildesk.common.enums.Platform;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
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
class BoardApplicationServiceCompositionTest {

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
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
    }

    @Test
    void getBoard_platformDistribution_and_recentActivity_followScopedKols() {
        KolDO tiktokKol = kol(Platform.TIKTOK, KolStage.REPLIED);
        KolDO instagramKol = kol(Platform.INSTAGRAM, KolStage.OUTREACH);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(tiktokKol, instagramKol));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        BoardSummaryDto result = service.getBoard("all", null, true, null, null, null, null);

        assertThat(result.platformDistribution()).hasSize(2);
        assertThat(result.platformDistribution())
                .extracting(segment -> segment.platform())
                .containsExactlyInAnyOrder("tiktok", "instagram");
        assertThat(result.recentActivity()).hasSizeLessThanOrEqualTo(16);
    }

    private static KolDO kol(Platform platform, KolStage stage) {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setEmail("creator@example.com");
        kol.setName("Creator");
        kol.setPrimaryPlatform(platform);
        kol.setStage(stage);
        kol.setReplyResolved(false);
        kol.setLastInboundAt(OffsetDateTime.now(ZoneOffset.UTC));
        return kol;
    }
}
