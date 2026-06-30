package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.WorkbenchResponseDto;
import com.lovart.maildesk.application.workbench.WorkbenchApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbench")
public class WorkbenchController {

    private final WorkbenchApplicationService workbench;

    public WorkbenchController(WorkbenchApplicationService workbench) {
        this.workbench = workbench;
    }

    @GetMapping
    public ResponseEntity<WorkbenchResponseDto> getWorkbench(
            @AuthenticationPrincipal SessionPrincipal principal,
            @RequestParam(required = false, defaultValue = "mine") String view,
            @RequestParam(required = false, defaultValue = "all") String stage,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int size
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        WorkbenchResponseDto body = workbench.getWorkbench(
                principal.userId(), view, stage, q, page, size);
        return ResponseEntity.ok(body);
    }
}
