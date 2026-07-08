package com.lovart.maildesk.application.board;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.BoardMemberRowDto;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceMembersTest {

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
    void getBoard_memberRows_useNeedsReplyNotStalled() {
        ProfileDO mentor = member(mentorId, "full_time", null, "Mentor");
        ProfileDO intern = member(internId, "intern", mentorId, "Intern");
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentor, intern));

        KolDO unreplied = unrepliedKol(mentorId);
        KolDO replied = repliedKol(mentorId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(unreplied, replied));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        BoardSummaryDto result = service.getBoard("all", null, true, null, null, null, null);

        assertThat(result.members()).hasSize(2);
        BoardMemberRowDto mentorRow = result.members().stream()
                .filter(row -> row.memberId().equals(mentorId))
                .findFirst()
                .orElseThrow();
        assertThat(mentorRow.unreplied()).isEqualTo(1);
        assertThat(mentorRow.total()).isEqualTo(2);
        assertThat(mentorRow.coveredMemberIds()).containsExactly(mentorId);
        assertThat(result.kpi().unrepliedKols()).isEqualTo(1);
    }

    @Test
    void getBoard_teamWide_excludeInterns_hidesInternRowsAndKols() {
        ProfileDO mentor = member(mentorId, "full_time", null, "Mentor");
        ProfileDO intern = member(internId, "intern", mentorId, "Intern");
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentor, intern));

        KolDO mentorKol = kol(mentorId, KolStage.OUTREACH);
        KolDO internKol = unrepliedKol(internId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentorKol, internKol));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        BoardSummaryDto result = service.getBoard("all", null, false, null, null, null, null);

        assertThat(result.members()).hasSize(1);
        assertThat(result.members().getFirst().memberId()).isEqualTo(mentorId);
        assertThat(result.kpi().totalKols()).isEqualTo(1);
        assertThat(result.kpi().unrepliedKols()).isEqualTo(0);
    }

    @Test
    void getBoard_ownerScope_aggregatesInternsOnMentorRow() {
        ProfileDO mentor = member(mentorId, "full_time", null, "Mentor");
        ProfileDO intern = member(internId, "intern", mentorId, "Intern");
        when(profiles.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentor, intern));

        KolDO mentorKol = kol(mentorId, KolStage.OUTREACH);
        KolDO internKol = unrepliedKol(internId);
        when(kols.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mentorKol, internKol));
        when(emails.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(emails.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        BoardSummaryDto result = service.getBoard("all", mentorId, true, null, null, null, null);

        assertThat(result.members()).hasSize(2);
        var mentorRow = result.members().stream()
                .filter(row -> row.memberId().equals(mentorId))
                .findFirst()
                .orElseThrow();
        assertThat(mentorRow.coveredMemberIds()).containsExactlyInAnyOrder(mentorId, internId);
        assertThat(mentorRow.total()).isEqualTo(2);
        assertThat(mentorRow.unreplied()).isEqualTo(1);
    }

    private static ProfileDO member(UUID id, String role, UUID mentorUserId, String name) {
        ProfileDO profile = new ProfileDO();
        profile.setId(id);
        profile.setRole(role);
        profile.setStatus("active");
        profile.setMentorUserId(mentorUserId);
        profile.setDisplayName(name);
        return profile;
    }

    private static KolDO kol(UUID ownerUserId, KolStage stage) {
        KolDO kol = new KolDO();
        kol.setId(UUID.randomUUID());
        kol.setOwnerUserId(ownerUserId);
        kol.setStage(stage);
        kol.setReplyResolved(false);
        return kol;
    }

    private static KolDO unrepliedKol(UUID ownerUserId) {
        KolDO kol = kol(ownerUserId, KolStage.OUTREACH);
        kol.setLastInboundAt(OffsetDateTime.now(ZoneOffset.UTC));
        return kol;
    }

    private static KolDO repliedKol(UUID ownerUserId) {
        KolDO kol = kol(ownerUserId, KolStage.REPLIED);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        kol.setLastInboundAt(now.minusDays(1));
        kol.setLastOutboundAt(now);
        return kol;
    }
}
