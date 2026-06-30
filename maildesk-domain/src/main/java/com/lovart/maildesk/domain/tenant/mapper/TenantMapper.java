package com.lovart.maildesk.domain.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lovart.maildesk.domain.tenant.entity.TenantDO;

/**
 * Mapper for the {@code tenants} table. Multi-tenant {@code WHERE tenant_id = ?}
 * injection is suppressed here because {@code tenants} is the tenancy root and
 * has no {@code tenant_id} column itself.
 */
public interface TenantMapper extends BaseMapper<TenantDO> {
}
