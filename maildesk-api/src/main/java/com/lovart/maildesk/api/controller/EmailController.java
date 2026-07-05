package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.EmailDto;
import com.lovart.maildesk.application.dto.EmailUpdateRequest;
import com.lovart.maildesk.application.email.EmailApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emails")
public class EmailController {

    private final EmailApplicationService emails;

    public EmailController(EmailApplicationService emails) {
        this.emails = emails;
    }

    @PatchMapping("/{emailId}")
    public ResponseEntity<EmailDto> updateEmail(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID emailId,
            @Valid @RequestBody EmailUpdateRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(emails.updateEmail(principal.userId(), emailId, request));
    }

    @DeleteMapping("/{emailId}")
    public ResponseEntity<Void> deleteEmail(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID emailId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        emails.deleteEmail(principal.userId(), emailId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{emailId}/reclassify")
    public ResponseEntity<EmailDto> reclassify(
            @AuthenticationPrincipal SessionPrincipal principal, @PathVariable UUID emailId) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(emails.reclassify(principal.userId(), emailId));
    }
}
