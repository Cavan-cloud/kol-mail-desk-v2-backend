package com.lovart.maildesk.application.dto;

import java.util.List;

public record TeamMembersResponseDto(
        List<TeamMemberDto> members,
        List<KolDto> pool
) {
}
