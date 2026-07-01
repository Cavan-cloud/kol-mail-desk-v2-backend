package com.lovart.maildesk.application.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovart.maildesk.application.dto.ProfileDto;
import com.lovart.maildesk.application.dto.TeamProfileUpdateRequest;
import com.lovart.maildesk.application.dto.TeamProfileUpdateResponse;
import com.lovart.maildesk.application.support.EntityMappers;
import com.lovart.maildesk.application.team.TeamApplicationService;
import com.lovart.maildesk.common.enums.UserRole;
import com.lovart.maildesk.common.enums.UserStatus;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ProfileApplicationService {

    private static final String GOOGLE_CREDENTIAL_TYPE = "google";

    private final ProfileMapper profiles;
    private final IntegrationCredentialMapper credentials;
    private final TeamApplicationService teamService;

    public ProfileApplicationService(
            ProfileMapper profiles,
            IntegrationCredentialMapper credentials,
            TeamApplicationService teamService) {
        this.profiles = profiles;
        this.credentials = credentials;
        this.teamService = teamService;
    }

    @Transactional(rollbackFor = Exception.class)
    public TeamProfileUpdateResponse updateOwnProfile(UUID userId, TeamProfileUpdateRequest request) {
        ProfileDO profile = profiles.selectById(userId);
        if (profile == null) {
            throw new BusinessException("NOT_FOUND", "用户资料不存在");
        }
        if (UserStatus.DEPARTED.dbValue().equals(profile.getStatus())) {
            throw new BusinessException("FORBIDDEN", "已离职账号无法修改资料");
        }

        UserRole role = request.role();
        if (role == UserRole.MEMBER) {
            throw new BusinessException("VALIDATION_ERROR", "无效的角色");
        }

        UUID mentorUserId = resolveMentorUserId(userId, role, request.mentorUserId());

        profile.setDisplayName(request.displayName().trim());
        profile.setRole(role.dbValue());
        profile.setMentorUserId(mentorUserId);
        profile.setFeishuOperatorName(normalizeFeishuOperatorName(request.feishuOperatorName()));

        if (UserStatus.PENDING_APPROVAL.dbValue().equals(profile.getStatus())) {
            profile.setStatus(UserStatus.ACTIVE.dbValue());
            profile.setApprovedAt(OffsetDateTime.now());
        }

        profiles.updateById(profile);
        ProfileDto dto = EntityMappers.toProfileDto(profile, hasGmailCredential(userId));
        int kolsAssigned = teamService.assignKolsByOperatorName(userId, profile.getFeishuOperatorName());
        return new TeamProfileUpdateResponse(dto, kolsAssigned);
    }

    private UUID resolveMentorUserId(UUID userId, UserRole role, UUID mentorUserId) {
        if (role != UserRole.INTERN) {
            if (mentorUserId != null) {
                throw new BusinessException("VALIDATION_ERROR", "仅实习生可设置 mentor");
            }
            return null;
        }
        if (mentorUserId == null) {
            return null;
        }
        if (mentorUserId.equals(userId)) {
            throw new BusinessException("VALIDATION_ERROR", "不能将自己设为 mentor");
        }
        ProfileDO mentor = profiles.selectById(mentorUserId);
        if (mentor == null || UserStatus.DEPARTED.dbValue().equals(mentor.getStatus())) {
            throw new BusinessException("VALIDATION_ERROR", "所选 mentor 不存在");
        }
        return mentorUserId;
    }

    private static String normalizeFeishuOperatorName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().replaceFirst("^@+", "");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasGmailCredential(UUID userId) {
        Long count = credentials.selectCount(
                new LambdaQueryWrapper<IntegrationCredentialDO>()
                        .eq(IntegrationCredentialDO::getUserId, userId)
                        .eq(IntegrationCredentialDO::getType, GOOGLE_CREDENTIAL_TYPE));
        return count != null && count > 0;
    }
}
