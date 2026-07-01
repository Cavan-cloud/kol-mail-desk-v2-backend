package com.lovart.maildesk.application.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lovart.maildesk.application.dto.KolDto;
import com.lovart.maildesk.application.dto.TeamMemberDto;
import com.lovart.maildesk.application.dto.TeamMembersResponseDto;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamApplicationService {

    private static final String GOOGLE_CREDENTIAL_TYPE = "google";

    private final ProfileMapper profiles;
    private final KolMapper kols;
    private final IntegrationCredentialMapper credentials;

    public TeamApplicationService(
            ProfileMapper profiles,
            KolMapper kols,
            IntegrationCredentialMapper credentials
    ) {
        this.profiles = profiles;
        this.kols = kols;
        this.credentials = credentials;
    }

    @Transactional(readOnly = true)
    public TeamMembersResponseDto listMembers() {
        List<ProfileDO> members = profiles.selectList(
                new LambdaQueryWrapper<ProfileDO>()
                        .ne(ProfileDO::getStatus, UserStatus.DEPARTED.dbValue())
                        .orderByAsc(ProfileDO::getDisplayName));

        Map<UUID, Integer> ownedCounts = countOwnedKols();
        Map<UUID, Boolean> gmailByUser = loadGmailAuthorization(members);

        List<TeamMemberDto> roster = members.stream()
                .map(p -> EntityMappers.toTeamMemberDto(
                        p,
                        gmailByUser.getOrDefault(p.getId(), false),
                        ownedCounts.getOrDefault(p.getId(), 0)
                ))
                .toList();

        List<KolDto> pool = kols.selectList(
                new LambdaQueryWrapper<KolDO>()
                        .in(KolDO::getStatus, "unassigned", "orphaned")
                        .orderByDesc(KolDO::getUpdatedAt))
                .stream()
                .map(EntityMappers::toKolDto)
                .toList();

        return new TeamMembersResponseDto(roster, pool);
    }

    private Map<UUID, Integer> countOwnedKols() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (KolDO kol : kols.selectList(new LambdaQueryWrapper<KolDO>().isNotNull(KolDO::getOwnerUserId))) {
            if (kol.getOwnerUserId() != null) {
                counts.merge(kol.getOwnerUserId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private Map<UUID, Boolean> loadGmailAuthorization(List<ProfileDO> members) {
        Map<UUID, Boolean> map = new HashMap<>();
        for (ProfileDO member : members) {
            Long count = credentials.selectCount(
                    new LambdaQueryWrapper<IntegrationCredentialDO>()
                            .eq(IntegrationCredentialDO::getUserId, member.getId())
                            .eq(IntegrationCredentialDO::getType, GOOGLE_CREDENTIAL_TYPE));
            map.put(member.getId(), count != null && count > 0);
        }
        return map;
    }

    /**
     * Claims unowned KOL rows whose {@code feishu_operator_name} matches the saved profile value.
     * Ported from legacy {@code app/api/team/profile/route.ts}.
     */
    @Transactional(rollbackFor = Exception.class)
    public int assignKolsByOperatorName(java.util.UUID userId, String feishuOperatorName) {
        if (userId == null || feishuOperatorName == null || feishuOperatorName.isBlank()) {
            return 0;
        }
        String normalized = FeishuCellExtractor.normalizeOperatorName(feishuOperatorName);
        if (normalized.isEmpty()) {
            return 0;
        }
        return kols.update(
                null,
                new UpdateWrapper<KolDO>()
                        .set("owner_user_id", userId)
                        .isNull("owner_user_id")
                        .apply("lower(replace(trim(feishu_operator_name), '@', '')) = {0}", normalized));
    }
}
