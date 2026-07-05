package com.lovart.maildesk.infrastructure.usage;

import com.lovart.maildesk.domain.usage.AiUsageEntry;
import com.lovart.maildesk.domain.usage.AiUsageLogPort;
import com.lovart.maildesk.domain.usage.entity.AiUsageLogDO;
import com.lovart.maildesk.domain.usage.mapper.AiUsageLogMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AiUsageLogService implements AiUsageLogPort {

    private final AiUsageLogMapper usageLogs;
    private final ObjectProvider<MeterRegistry> meterRegistry;

    public AiUsageLogService(AiUsageLogMapper usageLogs, ObjectProvider<MeterRegistry> meterRegistry) {
        this.usageLogs = usageLogs;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void record(AiUsageEntry entry) {
        AiUsageLogDO row = new AiUsageLogDO();
        row.setUserId(entry.userId());
        row.setCapability(entry.capability());
        row.setProvider(entry.provider() == null ? "unknown" : entry.provider());
        row.setModel(entry.model());
        row.setPromptTokens(entry.promptTokens());
        row.setCompletionTokens(entry.completionTokens());
        row.setDurationMs(entry.durationMs());
        row.setSuccess(entry.success());
        row.setEstimatedCostCny(entry.estimatedCostCny());
        usageLogs.insert(row);
        meterRegistry.ifAvailable(registry -> AiUsageMicrometer.record(registry, entry));
    }
}
