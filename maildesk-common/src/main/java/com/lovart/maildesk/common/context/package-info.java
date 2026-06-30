/**
 * Cross-cutting request-scoped context (tenant, user) backed by ThreadLocal.
 * <p>
 * Web filter / worker job entry points are responsible for populating these contexts
 * on the way in and clearing them on the way out (try / finally).
 */
package com.lovart.maildesk.common.context;
