package com.lovart.maildesk.application.gmail;

import com.lovart.maildesk.application.dto.BatchSendRequest;
import com.lovart.maildesk.application.dto.BatchSendResultDto;
import com.lovart.maildesk.application.dto.SendEmailRequest;
import com.lovart.maildesk.application.dto.SendEmailResultDto;
import com.lovart.maildesk.common.exception.BusinessException;
import com.lovart.maildesk.common.exception.GmailIntegrationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GmailSendApplicationService {

    private static final long BATCH_DELAY_MS = 1_200L;

    private final GmailSendExecutor executor;

    public GmailSendApplicationService(GmailSendExecutor executor) {
        this.executor = executor;
    }

    public SendEmailResultDto send(UUID userId, SendEmailRequest request) {
        return executor.execute(userId, request);
    }

    public BatchSendResultDto batchSend(UUID userId, BatchSendRequest request) {
        List<SendEmailRequest> items = request.items();
        List<SendEmailResultDto> results = new ArrayList<>(items.size());

        for (int index = 0; index < items.size(); index++) {
            results.add(executeBatchItem(userId, items.get(index)));
            if (index < items.size() - 1) {
                sleep(BATCH_DELAY_MS);
            }
        }

        int successCount = 0;
        int failedCount = 0;
        for (SendEmailResultDto result : results) {
            if ("sent".equals(result.status())) {
                successCount++;
            } else if ("failed".equals(result.status()) || "not_configured".equals(result.status())) {
                failedCount++;
            }
        }
        return new BatchSendResultDto(successCount, failedCount, results);
    }

    private SendEmailResultDto executeBatchItem(UUID userId, SendEmailRequest item) {
        try {
            return executor.execute(userId, item);
        } catch (GmailIntegrationException ex) {
            if (ex.credentialExpired()) {
                throw ex;
            }
            return new SendEmailResultDto("failed", null, ex.getMessage());
        } catch (BusinessException ex) {
            return new SendEmailResultDto("failed", null, ex.getMessage());
        }
    }

    private static void sleep(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
