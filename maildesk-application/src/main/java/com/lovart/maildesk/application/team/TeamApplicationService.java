package com.lovart.maildesk.application.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lovart.maildesk.application.audit.AuditLogService;
import com.lovart.maildesk.application.dto.DepartTeamMemberResult;
import com.lovart.maildesk.application.dto.KolAssignRequest;
import com.lovart.maildesk.application.dto.KolAssignResult;
import com.lovart.maildesk.application.dto.KolDto;
import com.lovart.maildesk.application.dto.TeamMemberDto;
import com.lovart.maildesk.application.dto.TeamMembersResponseDto;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.application.support.WorkbenchRules;
import com.lovart.maildesk.common.enums.ActionType;
import com.lovart.maildesk.common.enums.KolStatus;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.common.feishu.FeishuCellExtractor;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.email.entity.EmailDO;
import com.lovart.maildesk.domain.email.mapper.EmailMapper;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.kol.mapper.KolMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TeamApplicationService {

    private static final String GOOGLE_CREDENTIAL_TYPE = "google";
    private static final String SOURCE_GMAIL = "gmail";
    private static final String SOURCE_FEISHU = "feishu";

    private final ProfileMapper profiles;
    private final KolMapper kols;
    private final EmailMapper emails;
    private final IntegrationCredentialMapper credentials;
    private final AuditLogService auditLog;

    public TeamApplicationService(
            ProfileMapper profiles,
            KolMapper kols,
            EmailMapper emails,
            IntegrationCredentialMapper credentials,
            AuditLogService auditLog
    ) {
        this.profiles = profiles;
        this.kols = kols;
        this.emails = emails;
        this.credentials = credentials;
        this.auditLog = auditLog;
    }

    public record OperatorClaimResult(int released, int assigned) {
    }

    @Transactional(readOnly = true)
    public TeamMembersResponseDto listMembers() {
        List<ProfileDO> members = profiles.selectList(
                new LambdaQueryWrapper<ProfileDO>()
                        .orderByAsc(ProfileDO::getDisplayName));
        members.sort(Comparator.comparing(member ->
                UserStatus.DEPARTED.dbValue().equals(member.getStatus()) ? 1 : 0));

        Map<UUID, Integer> ownedCounts = countOwnedKols();
        Map<UUID, MemberKolStats> memberStats = computeMemberKolStats();
        Map<UUID, Boolean> gmailByUser = loadGmailAuthorization(members);

        List<TeamMemberDto> roster = members.stream()
                .map(p -> {
                    MemberKolStats stats = memberStats.getOrDefault(p.getId(), MemberKolStats.EMPTY);
                    return EntityMappers.toTeamMemberDto(
                            p,
                            gmailByUser.getOrDefault(p.getId(), false),
                            ownedCounts.getOrDefault(p.getId(), 0),
                            stats.active(),
                            stats.closed(),
                            stats.stalled());
                })
                .toList();

        List<KolDto> pool = kols.selectList(
                new LambdaQueryWrapper<KolDO>()
                        .in(KolDO::getStatus, KolStatus.UNASSIGNED, KolStatus.ORPHANED)
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

    private Map<UUID, MemberKolStats> computeMemberKolStats() {
        Map<UUID, MemberKolStats> stats = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (KolDO kol : kols.selectList(new LambdaQueryWrapper<KolDO>().isNotNull(KolDO::getOwnerUserId))) {
            UUID ownerId = kol.getOwnerUserId();
            if (ownerId == null) {
                continue;
            }
            MemberKolStats current = stats.getOrDefault(ownerId, MemberKolStats.EMPTY);
            int active = current.active();
            int closed = current.closed();
            int stalled = current.stalled();
            if (KolStatus.ACTIVE.equals(kol.getStatus())) {
                active++;
            }
            if (KolStatus.CLOSED.equals(kol.getStatus())) {
                closed++;
            }
            if (WorkbenchRules.isUnreplied(kol, now)) {
                stalled++;
            }
            stats.put(ownerId, new MemberKolStats(active, closed, stalled));
        }
        return stats;
    }

    private record MemberKolStats(int active, int closed, int stalled) {
        private static final MemberKolStats EMPTY = new MemberKolStats(0, 0, 0);
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
     * Releases premature gmail duplicate / empty-operator claims, then claims rows matching the profile.
     */
    @Transactional(rollbackFor = Exception.class)
    public OperatorClaimResult reconcileAndAssignKolsByOperatorName(UUID userId, String feishuOperatorName) {
        if (userId == null || feishuOperatorName == null || feishuOperatorName.isBlank()) {
            return new OperatorClaimResult(0, 0);
        }
        String normalized = FeishuCellExtractor.normalizeOperatorName(feishuOperatorName);
        if (normalized.isEmpty()) {
            return new OperatorClaimResult(0, 0);
        }
        int released = releasePrematureOwnedKols(userId, normalized);
        int assigned = assignKolsByOperatorName(userId, feishuOperatorName);
        return new OperatorClaimResult(released, assigned);
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
                        .apply(
                                "regexp_replace(lower(replace(trim(feishu_operator_name), '@', '')), '\\s', '', 'g') = {0}",
                                normalized));
    }

    private int releasePrematureOwnedKols(UUID userId, String normalizedOperator) {
        List<KolDO> owned = kols.selectList(
                new LambdaQueryWrapper<KolDO>().eq(KolDO::getOwnerUserId, userId));
        int released = 0;
        for (KolDO kol : owned) {
            if (matchesOperatorName(kol.getFeishuOperatorName(), normalizedOperator)) {
                continue;
            }
            boolean gmailDuplicate = SOURCE_GMAIL.equals(kol.getSource());
            boolean emptyOperator = FeishuCellExtractor.normalizeOperatorName(kol.getFeishuOperatorName()).isEmpty();
            if (!gmailDuplicate && !emptyOperator) {
                continue;
            }
            migrateEmailsToMatchingFeishuRow(userId, kol, normalizedOperator);
            KolDO patch = new KolDO();
            patch.setId(kol.getId());
            patch.setOwnerUserId(null);
            kols.updateById(patch);
            released++;
        }
        return released;
    }

    private void migrateEmailsToMatchingFeishuRow(UUID userId, KolDO fromKol, String normalizedOperator) {
        if (fromKol.getEmail() == null || fromKol.getEmail().isBlank()) {
            return;
        }
        String normalizedEmail = fromKol.getEmail().trim().toLowerCase();
        List<KolDO> siblings = kols.selectList(new LambdaQueryWrapper<KolDO>()
                .apply("normalized_email = {0}", normalizedEmail)
                .ne(KolDO::getId, fromKol.getId()));
        KolDO target = siblings.stream()
                .filter(k -> SOURCE_FEISHU.equals(k.getSource()) || k.getFeishuRecordId() != null)
                .filter(k -> matchesOperatorName(k.getFeishuOperatorName(), normalizedOperator))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return;
        }
        emails.update(
                null,
                new UpdateWrapper<EmailDO>()
                        .set("kol_id", target.getId())
                        .eq("kol_id", fromKol.getId())
                        .eq("user_id", userId));
    }

    private static boolean matchesOperatorName(String stored, String normalizedOperator) {
        return FeishuCellExtractor.normalizeOperatorName(stored).equals(normalizedOperator);
    }

    /**
     * Leader marks a member as departed and orphans their active KOLs.
     */
    @Transactional(rollbackFor = Exception.class)
    public DepartTeamMemberResult departMember(UUID actorUserId, UUID targetUserId) {
        assertLeader(actorUserId);
        if (actorUserId.equals(targetUserId)) {
            throw new BusinessException("FORBIDDEN", "不能将自己标记离职");
        }

        ProfileDO target = profiles.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException("NOT_FOUND", "成员不存在");
        }
        if (UserStatus.DEPARTED.dbValue().equals(target.getStatus())) {
            throw new BusinessException("CONFLICT", "该成员已处于离职状态");
        }

        OffsetDateTime departedAt = OffsetDateTime.now();
        ProfileDO profilePatch = new ProfileDO();
        profilePatch.setId(targetUserId);
        profilePatch.setStatus(UserStatus.DEPARTED.dbValue());
        profilePatch.setDepartedAt(departedAt);
        profiles.updateById(profilePatch);

        int orphanedCount = kols.update(
                null,
                new UpdateWrapper<KolDO>()
                        .set("status", KolStatus.ORPHANED)
                        .eq("owner_user_id", targetUserId)
                        .eq("status", KolStatus.ACTIVE));

        auditLog.append(
                ActionType.USER_DEPARTED,
                "user",
                targetUserId,
                Map.of(
                        "pool_kols", true,
                        "orphaned_count", orphanedCount));

        return new DepartTeamMemberResult(orphanedCount);
    }

    /**
     * Leader reassigns orphaned KOLs (left by departed members) to an active member.
     */
    @Transactional(rollbackFor = Exception.class)
    public KolAssignResult assignPoolKols(UUID actorUserId, KolAssignRequest request) {
        assertLeader(actorUserId);

        List<UUID> kolIds = request.kolIds().stream().distinct().toList();
        ProfileDO assignee = profiles.selectById(request.ownerUserId());
        if (assignee == null || UserStatus.DEPARTED.dbValue().equals(assignee.getStatus())) {
            throw new BusinessException("VALIDATION_ERROR", "被分配成员不存在或非在职状态");
        }
        if (!UserStatus.ACTIVE.dbValue().equals(assignee.getStatus())) {
            throw new BusinessException("VALIDATION_ERROR", "被分配成员不存在或非在职状态");
        }

        List<KolDO> rows = kols.selectList(
                new LambdaQueryWrapper<KolDO>().in(KolDO::getId, kolIds));
        Set<UUID> foundIds = new HashSet<>();
        for (KolDO row : rows) {
            foundIds.add(row.getId());
        }
        List<UUID> missingIds = kolIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException("NOT_FOUND", "部分达人不存在");
        }

        List<UUID> conflictIds = new ArrayList<>();
        for (KolDO row : rows) {
            if (!isPoolAssignable(row.getStatus())) {
                conflictIds.add(row.getId());
            }
        }
        if (!conflictIds.isEmpty()) {
            throw new BusinessException(
                    "CONFLICT",
                    "只能分配团队池中的达人（无主 unassigned 或离职遗留 orphaned），其余达人不可分配");
        }

        UUID assigneeUserId = request.ownerUserId();
        for (KolDO row : rows) {
            KolDO patch = new KolDO();
            patch.setId(row.getId());
            patch.setOwnerUserId(assigneeUserId);
            patch.setStatus(KolStatus.ACTIVE);
            kols.updateById(patch);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("from_owner", row.getOwnerUserId() == null ? null : row.getOwnerUserId().toString());
            metadata.put("to_owner", assigneeUserId.toString());
            metadata.put("from_status", row.getStatus() == null ? null : row.getStatus().dbValue());
            metadata.put("to_status", KolStatus.ACTIVE.dbValue());
            metadata.put("assigned_by", actorUserId.toString());
            auditLog.append(ActionType.OWNER_CHANGE, "kol", row.getId(), metadata);
        }

        return new KolAssignResult(rows.size());
    }

    private static boolean isPoolAssignable(KolStatus status) {
        return KolStatus.UNASSIGNED.equals(status) || KolStatus.ORPHANED.equals(status);
    }

    private void assertLeader(UUID actorUserId) {
        ProfileDO actor = profiles.selectById(actorUserId);
        if (actor == null || UserStatus.DEPARTED.dbValue().equals(actor.getStatus())) {
            throw new BusinessException("FORBIDDEN", "无权执行该操作");
        }
        if (UserRole.LEADER != UserRole.fromDbValue(actor.getRole())) {
            throw new BusinessException("FORBIDDEN", "仅 Leader 可执行该操作");
        }
    }
}
