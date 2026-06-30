package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.application.dto.TemplateListResponseDto;
import com.lovart.maildesk.application.template.TemplateApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateApplicationService templates;

    public TemplateController(TemplateApplicationService templates) {
        this.templates = templates;
    }

    @GetMapping
    public ResponseEntity<TemplateListResponseDto> listTemplates() {
        return ResponseEntity.ok(templates.listTemplates());
    }
}
