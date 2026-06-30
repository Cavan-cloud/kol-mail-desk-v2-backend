package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.KolDetailDto;
import com.lovart.maildesk.application.workbench.WorkbenchApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kols")
public class KolController {

    private final WorkbenchApplicationService workbench;

    public KolController(WorkbenchApplicationService workbench) {
        this.workbench = workbench;
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
}
