package com.lovart.maildesk.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/gmail/batch-send}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchSendRequest(
        @NotEmpty(message = "批量发送至少包含一封邮件")
        @Size(max = 25, message = "批量发送不能超过 25 封")
        List<@Valid SendEmailRequest> items
) {}
