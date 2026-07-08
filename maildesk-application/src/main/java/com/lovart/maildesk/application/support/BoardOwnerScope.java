package com.lovart.maildesk.application.support;

import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.kol.entity.KolDO;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves board owner perspective (v1 {@code board-data.ts} scopedOwnerIds).
 */
public final class BoardOwnerScope {

    private BoardOwnerScope() {
    }

    /**
     * @return {@code null} when no owner filter (team-wide); otherwise owner + optional interns.
     */
    public static Set<UUID> resolveScopeOwnerIds(UUID ownerId, boolean includeInterns, List<ProfileDO> members) {
        if (ownerId == null) {
            return null;
        }
        ProfileDO owner = members.stream()
                .filter(member -> ownerId.equals(member.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "成员不存在"));
        Set<UUID> scope = new HashSet<>();
        scope.add(owner.getId());
        if (includeInterns) {
            for (ProfileDO member : members) {
                if (UserRole.INTERN.dbValue().equals(member.getRole())
                        && owner.getId().equals(member.getMentorUserId())) {
                    scope.add(member.getId());
                }
            }
        }
        return scope;
    }

    public static boolean matchesOwnerScope(KolDO kol, Set<UUID> scopeOwnerIds) {
        if (scopeOwnerIds == null) {
            return true;
        }
        UUID ownerUserId = kol.getOwnerUserId();
        return ownerUserId != null && scopeOwnerIds.contains(ownerUserId);
    }

    public static List<ProfileDO> activeBoardMembers(List<ProfileDO> members) {
        return members.stream()
                .filter(member -> !UserStatus.DEPARTED.dbValue().equals(member.getStatus()))
                .toList();
    }

    public static List<UUID> internIdsFor(UUID mentorId, List<ProfileDO> members) {
        List<UUID> internIds = new ArrayList<>();
        for (ProfileDO member : members) {
            if (UserRole.INTERN.dbValue().equals(member.getRole()) && mentorId.equals(member.getMentorUserId())) {
                internIds.add(member.getId());
            }
        }
        return internIds;
    }

    public static Set<UUID> internMemberIds(List<ProfileDO> members) {
        Set<UUID> internIds = new HashSet<>();
        for (ProfileDO member : members) {
            if (UserRole.INTERN.dbValue().equals(member.getRole())) {
                internIds.add(member.getId());
            }
        }
        return internIds;
    }

    public static boolean matchesBoardKolScope(
            KolDO kol,
            Set<UUID> scopeOwnerIds,
            boolean includeInterns,
            Set<UUID> internOwnerIds
    ) {
        if (scopeOwnerIds != null) {
            return matchesOwnerScope(kol, scopeOwnerIds);
        }
        if (!includeInterns) {
            UUID ownerUserId = kol.getOwnerUserId();
            return ownerUserId == null || !internOwnerIds.contains(ownerUserId);
        }
        return true;
    }

    public static List<ProfileDO> visibleBoardMembers(
            UUID ownerId,
            boolean includeInterns,
            List<ProfileDO> members
    ) {
        if (ownerId == null) {
            if (!includeInterns) {
                return members.stream()
                        .filter(member -> !UserRole.INTERN.dbValue().equals(member.getRole()))
                        .toList();
            }
            return members;
        }
        ProfileDO owner = members.stream()
                .filter(member -> ownerId.equals(member.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "成员不存在"));
        List<UUID> internIds = internIdsFor(owner.getId(), members);
        return members.stream()
                .filter(member -> member.getId().equals(owner.getId())
                        || (includeInterns && internIds.contains(member.getId())))
                .toList();
    }
}
