package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.EmailTemplateDto;
import com.lovart.maildesk.application.dto.TemplateListResponseDto;
import com.lovart.maildesk.application.dto.TemplateUpsertRequest;
import com.lovart.maildesk.application.template.TemplateApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateApplicationService templates;

    public TemplateController(TemplateApplicationService templates) {
        this.templates = templates;
    }

    @GetMapping
    public ResponseEntity<TemplateListResponseDto> listTemplates(
            @AuthenticationPrincipal SessionPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(templates.listTemplates(principal.userId()));
    }

    @PostMapping
    public ResponseEntity<EmailTemplateDto> createTemplate(
            @AuthenticationPrincipal SessionPrincipal principal,
            @Valid @RequestBody TemplateUpsertRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templates.createTemplate(principal.userId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> updateTemplate(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody TemplateUpsertRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(templates.updateTemplate(principal.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @AuthenticationPrincipal SessionPrincipal principal,
            @PathVariable UUID id
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        templates.deleteTemplate(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
