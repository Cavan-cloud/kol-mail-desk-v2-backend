package com.lovart.maildesk.domain.scheduled.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lovart.maildesk.domain.scheduled.entity.ScheduledEmailDO;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ScheduledEmailMapper extends BaseMapper<ScheduledEmailDO> {

    /**
     * Atomically claim due rows for dispatch ({@code FOR UPDATE SKIP LOCKED}).
     * Increments {@code attempt_count} and sets {@code status=processing}.
     * Tenant filter is explicit in XML — {@code TenantLineInnerInterceptor} breaks
     * {@code ORDER BY … FOR UPDATE SKIP LOCKED} clause ordering.
     */
    @InterceptorIgnore(tenantLine = "true")
    List<ScheduledEmailDO> claimDueBatch(
            @Param("now") OffsetDateTime now,
            @Param("limit") int limit,
            @Param("tenantId") UUID tenantId);

    /**
     * Seconds between {@code now} and the oldest overdue {@code scheduled} row (0 if none).
     * Tenant filter is explicit in XML (see {@link #claimDueBatch}).
     */
    @InterceptorIgnore(tenantLine = "true")
    Double selectMaxDispatchLagSeconds(
            @Param("now") OffsetDateTime now,
            @Param("tenantId") UUID tenantId);
}
