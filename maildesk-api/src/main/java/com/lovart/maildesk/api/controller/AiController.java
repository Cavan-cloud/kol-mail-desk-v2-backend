package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.ai.AiApplicationService;
import com.lovart.maildesk.application.dto.AiCheckRequest;
import com.lovart.maildesk.application.dto.AiCheckResult;
import com.lovart.maildesk.application.dto.AiDraftRequest;
import com.lovart.maildesk.application.dto.AiDraftResult;
import com.lovart.maildesk.application.dto.AiResultEnvelope;
import com.lovart.maildesk.application.dto.AiTranslateRequest;
import com.lovart.maildesk.application.dto.AiTranslateResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiApplicationService ai;

    public AiController(AiApplicationService ai) {
        this.ai = ai;
    }

    @PostMapping("/draft")
    public ResponseEntity<AiResultEnvelope<AiDraftResult>> draft(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody AiDraftRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AiResultEnvelope<>(ai.generateDraft(request)));
    }

    @PostMapping("/check")
    public ResponseEntity<AiResultEnvelope<AiCheckResult>> check(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody AiCheckRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AiResultEnvelope<>(ai.checkDraft(request)));
    }

    @PostMapping("/translate")
    public ResponseEntity<AiResultEnvelope<AiTranslateResult>> translate(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody AiTranslateRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AiResultEnvelope<>(ai.translate(request)));
    }
}
