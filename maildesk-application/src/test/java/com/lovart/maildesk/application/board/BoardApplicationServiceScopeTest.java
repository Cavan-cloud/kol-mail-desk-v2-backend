package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.common.enums.KolStage;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceScopeTest {

    @Mock
    private KolMapper kols;

    @Mock
    private EmailMapper emails;

    @Mock
    private ProfileMapper profiles;

    private BoardApplicationService service;
    private UUID mentorId;
    private UUID internId;

    @BeforeEach
    void setUp() {
        service = new BoardApplicationService(kols, emails, profiles);
        mentorId = UUID.randomUUID();
        internId = UUID.randomUUID();
    }

    @Test
    void getBoard_ownerScope_limitsKpiToOwnedKols() {
        ProfileDO mentor = member(mentorId, "full_time", null);
        ProfileDO intern = member(internId, "intern", mentorId);
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentor, intern));

        KolDO mentorKol = kol(mentorId, KolStage.OUTREACH);
        KolDO internKol = kol(internId, KolStage.REPLIED);
        KolDO otherKol = kol(UUID.randomUUID(), KolStage.OUTREACH);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentorKol, internKol, otherKol));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BoardSummaryDto teamWide = service.getBoard("all", null, true, null, null, null, null);
        BoardSummaryDto mentorScope = service.getBoard("all", mentorId, true, null, null, null, null);
        BoardSummaryDto mentorOnly = service.getBoard("all", mentorId, false, null, null, null, null);

        assertThat(teamWide.kpi().totalKols()).isEqualTo(3);
        assertThat(mentorScope.kpi().totalKols()).isEqualTo(2);
        assertThat(mentorOnly.kpi().totalKols()).isEqualTo(1);
        assertThat(mentorScope.selectedOwnerId()).isEqualTo(mentorId);
        assertThat(mentorScope.includeInterns()).isTrue();

        BoardSummaryDto teamExcludeInterns = service.getBoard("all", null, false, null, null, null, null);
        assertThat(teamExcludeInterns.kpi().totalKols()).isEqualTo(2);
        assertThat(teamExcludeInterns.members()).hasSize(1);
        assertThat(teamExcludeInterns.members().getFirst().memberId()).isEqualTo(mentorId);
    }

    private static ProfileDO member(UUID id, String role, UUID mentorUserId) {
        ProfileDO profile = new ProfileDO();
        profile.setId(id);
        profile.setRole(role);
        profile.setStatus("active");
        profile.setMentorUserId(mentorUserId);
        return profile;
    }

    private static KolDO kol(UUID ownerUserId, KolStage stage) {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setOwnerUserId(ownerUserId);
        kol.setStage(stage);
        return kol;
    }
}
