package com.lovart.maildesk.application.dto;

import com.lovart.maildesk.common.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code PATCH /api/v1/team/profile}.
 */
public record TeamProfileUpdateRequest(
        @NotBlank(message = "请填写显示名")
        String displayName,
        @NotNull(message = "请选择角色")
        UserRole role,
        UUID mentorUserId,
        String feishuOperatorName
) {
}
