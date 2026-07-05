package com.lovart.maildesk.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record KolAssignRequest(
        @NotEmpty(message = "至少选择一个达人") List<UUID> kolIds,
        @NotNull(message = "请选择接收成员") UUID ownerUserId
) {
}
