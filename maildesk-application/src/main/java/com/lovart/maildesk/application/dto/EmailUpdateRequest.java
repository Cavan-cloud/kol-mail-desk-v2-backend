package com.lovart.maildesk.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lovart.maildesk.common.enums.KolStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/v1/emails/{emailId}}.
 */
public record EmailUpdateRequest(
        @NotNull(message = "请指定已读状态")
        Boolean isRead
) {
}
