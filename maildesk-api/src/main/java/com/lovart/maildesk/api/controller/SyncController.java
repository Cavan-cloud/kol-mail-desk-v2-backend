package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.dto.GmailSyncRequestBody;
import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.FeishuSyncStatusDto;
import com.lovart.maildesk.application.dto.GmailSyncStatusDto;
import com.lovart.maildesk.application.sync.feishu.FeishuSyncApplicationService;
import com.lovart.maildesk.application.sync.gmail.GmailSyncApplicationService;
import com.lovart.maildesk.application.sync.gmail.GmailSyncOptions;
import com.lovart.maildesk.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final FeishuSyncApplicationService feishuSync;
    private final GmailSyncApplicationService gmailSync;

    public SyncController(
            FeishuSyncApplicationService feishuSync, GmailSyncApplicationService gmailSync) {
        this.feishuSync = feishuSync;
        this.gmailSync = gmailSync;
    }

    @PostMapping("/feishu")
    public ResponseEntity<FeishuSyncStatusDto> triggerFeishuSync(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            FeishuSyncStatusDto status = feishuSync.triggerSync();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(status);
        } catch (BusinessException ex) {
            if ("CONFLICT".equals(ex.errorCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(feishuSync.getStatus());
            }
            throw ex;
        }
    }

    @GetMapping("/feishu/status")
    public ResponseEntity<FeishuSyncStatusDto> getFeishuSyncStatus(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(feishuSync.getStatus());
    }

    @PostMapping("/gmail")
    public ResponseEntity<GmailSyncStatusDto> triggerGmailSync(
            @AuthenticationPrincipal SessionPrincipal principal,
            @RequestBody(required = false) GmailSyncRequestBody body) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GmailSyncOptions options = toOptions(body);
        try {
            GmailSyncStatusDto status = gmailSync.triggerSync(principal.userId(), options);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(status);
        } catch (BusinessException ex) {
            if ("CONFLICT".equals(ex.errorCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(gmailSync.getStatus(principal.userId()));
            }
            throw ex;
        }
    }

    @GetMapping("/gmail/status")
    public ResponseEntity<GmailSyncStatusDto> getGmailSyncStatus(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gmailSync.getStatus(principal.userId()));
    }

    private static GmailSyncOptions toOptions(GmailSyncRequestBody body) {
        if (body == null || body.mode() == null || body.mode().isBlank() || "incremental".equals(body.mode())) {
            return GmailSyncOptions.incremental();
        }
        if ("history".equals(body.mode())) {
            return GmailSyncOptions.historyPage(body.pageToken());
        }
        return GmailSyncOptions.incremental();
    }
}
