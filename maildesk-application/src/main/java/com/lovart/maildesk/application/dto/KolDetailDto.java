package com.lovart.maildesk.application.dto;

import java.util.List;

public record KolDetailDto(
        KolDto kol,
        String ownerName,
        List<EmailDto> emails
) {
}
