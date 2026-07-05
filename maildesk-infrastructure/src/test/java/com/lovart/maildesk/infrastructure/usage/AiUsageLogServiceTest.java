package com.lovart.maildesk.infrastructure.usage;

import com.lovart.maildesk.domain.usage.AiUsageEntry;
import com.lovart.maildesk.domain.usage.entity.AiUsageLogDO;
import com.lovart.maildesk.domain.usage.mapper.AiUsageLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiUsageLogServiceTest {

    @Mock
    private AiUsageLogMapper usageLogs;

    @Mock
    private ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider;

    @Test
    void mapsEntryToEntity() {
        AiUsageLogService service = new AiUsageLogService(usageLogs, meterRegistryProvider);
        UUID userId = UUID.randomUUID();

        service.record(new AiUsageEntry(
                userId,
                "classify",
                "moonshot",
                "moonshot-v1-8k",
                100,
                50,
                120,
                true,
                new BigDecimal("0.00180000")));

        ArgumentCaptor<AiUsageLogDO> captor = ArgumentCaptor.forClass(AiUsageLogDO.class);
        verify(usageLogs).insert(captor.capture());
        verify(meterRegistryProvider).ifAvailable(any());
        AiUsageLogDO row = captor.getValue();
        assertThat(row.getUserId()).isEqualTo(userId);
        assertThat(row.getCapability()).isEqualTo("classify");
        assertThat(row.getProvider()).isEqualTo("moonshot");
        assertThat(row.getPromptTokens()).isEqualTo(100);
        assertThat(row.getSuccess()).isTrue();
        assertThat(row.getEstimatedCostCny()).isEqualByComparingTo("0.00180000");
    }
}
