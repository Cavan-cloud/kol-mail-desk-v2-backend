package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.application.dto.FeishuSyncStatusDto;
import com.lovart.maildesk.application.sync.feishu.FeishuSyncApplicationService;
import com.lovart.maildesk.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final FeishuSyncApplicationService feishuSync;

    public SyncController(FeishuSyncApplicationService feishuSync) {
        this.feishuSync = feishuSync;
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
}
