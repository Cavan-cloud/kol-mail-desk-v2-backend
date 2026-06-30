package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.ScheduledEmailListResponseDto;
import com.lovart.maildesk.application.scheduled.ScheduledEmailApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduled-emails")
public class ScheduledEmailController {

    private final ScheduledEmailApplicationService scheduledEmails;

    public ScheduledEmailController(ScheduledEmailApplicationService scheduledEmails) {
        this.scheduledEmails = scheduledEmails;
    }

    @GetMapping
    public ResponseEntity<ScheduledEmailListResponseDto> listScheduledEmails(
            @AuthenticationPrincipal SessionPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(scheduledEmails.listForUser(principal.userId()));
    }
}
