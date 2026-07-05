package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.BatchSendRequest;
import com.lovart.maildesk.application.dto.BatchSendResultDto;
import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
import com.lovart.maildesk.application.gmail.GmailSendApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gmail")
public class GmailController {

    private final GmailSendApplicationService gmailSend;

    public GmailController(GmailSendApplicationService gmailSend) {
        this.gmailSend = gmailSend;
    }

    @PostMapping("/send")
    public ResponseEntity<SendEmailResultDto> send(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody SendEmailRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gmailSend.send(principal.userId(), request));
    }

    @PostMapping("/batch-send")
    public ResponseEntity<BatchSendResultDto> batchSend(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody BatchSendRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gmailSend.batchSend(principal.userId(), request));
    }
}
