package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.ScheduledEmailCreateRequest;
import com.lovart.maildesk.application.dto.ScheduledEmailDto;
import com.lovart.maildesk.application.dto.ScheduledEmailListResponseDto;
import com.lovart.maildesk.application.scheduled.ScheduledEmailApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @PostMapping
    public ResponseEntity<ScheduledEmailDto> createScheduledEmail(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody ScheduledEmailCreateRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduledEmails.create(principal.userId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelScheduledEmail(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID id
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        scheduledEmails.cancel(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
