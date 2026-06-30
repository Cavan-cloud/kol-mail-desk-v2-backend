/**
 * Tenant domain — the multi-tenancy root row. The {@code tenants} table itself is
 * exempt from the {@code tenant_id} column convention and from
 * {@code TenantLineInnerInterceptor} injection (see {@code MaildeskTenantLineHandler}).
 */
package com.lovart.maildesk.domain.tenant;
