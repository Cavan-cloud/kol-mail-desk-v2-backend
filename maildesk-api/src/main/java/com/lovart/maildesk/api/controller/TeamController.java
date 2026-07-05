package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.DepartTeamMemberResult;
import com.lovart.maildesk.application.dto.TeamMembersResponseDto;
import com.lovart.maildesk.application.dto.TeamProfileUpdateRequest;
import com.lovart.maildesk.application.dto.TeamProfileUpdateResponse;
import com.lovart.maildesk.application.profile.ProfileApplicationService;
import com.lovart.maildesk.application.team.TeamApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Team roster + profile onboarding (P1-T05 / P1-T08).
 */
@RestController
@RequestMapping("/api/v1/team")
public class TeamController {

    private final ProfileApplicationService profileService;
    private final TeamApplicationService teamService;

    public TeamController(ProfileApplicationService profileService, TeamApplicationService teamService) {
        this.profileService = profileService;
        this.teamService = teamService;
    }

    @GetMapping("/members")
    public ResponseEntity<TeamMembersResponseDto> listMembers() {
        return ResponseEntity.ok(teamService.listMembers());
    }

    @PatchMapping("/profile")
    public ResponseEntity<TeamProfileUpdateResponse> updateProfile(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody TeamProfileUpdateRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        TeamProfileUpdateResponse updated = profileService.updateOwnProfile(principal.userId(), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/depart/{userId}")
    public ResponseEntity<DepartTeamMemberResult> departMember(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID userId
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(teamService.departMember(principal.userId(), userId));
    }
}
