package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.KolAssignRequest;
import com.lovart.maildesk.application.dto.KolAssignResult;
import com.lovart.maildesk.application.dto.KolDetailDto;
import com.lovart.maildesk.application.dto.KolDto;
import com.lovart.maildesk.application.dto.KolUpdateRequest;
import com.lovart.maildesk.application.kol.KolApplicationService;
import com.lovart.maildesk.application.team.TeamApplicationService;
import com.lovart.maildesk.application.workbench.WorkbenchApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kols")
public class KolController {

    private final WorkbenchApplicationService workbench;
    private final KolApplicationService kolService;
    private final TeamApplicationService teamService;

    public KolController(
            WorkbenchApplicationService workbench,
            KolApplicationService kolService,
            TeamApplicationService teamService
    ) {
        this.workbench = workbench;
        this.kolService = kolService;
        this.teamService = teamService;
    }

    @GetMapping("/{kolId}")
    public ResponseEntity<KolDetailDto> getKolDetail(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID kolId
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(workbench.getKolDetail(principal.userId(), kolId));
    }

    @PatchMapping("/{kolId}")
    public ResponseEntity<KolDto> updateKol(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID kolId,
            @Valid @RequestBody KolUpdateRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(kolService.updateKol(principal.userId(), kolId, request));
    }

    @PostMapping("/assign")
    public ResponseEntity<KolAssignResult> assignKols(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody KolAssignRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(teamService.assignPoolKols(principal.userId(), request));
    }
}
