/**
 * Shared MyBatis {@link org.apache.ibatis.type.BaseTypeHandler} implementations
 * for PostgreSQL-specific column types (JSONB, TEXT[], ENUM).
 * <p>
 * Wired in {@code MyBatisPlusConfig} via a {@code ConfigurationCustomizer} so
 * consumers do not need to attach {@code @TableField(typeHandler = ...)} on
 * every field. Concrete subclasses of {@link com.lovart.maildesk.common.typehandler.PgEnumTypeHandler}
 * pin a single Java enum to a single PostgreSQL ENUM type name.
 */
package com.lovart.maildesk.common.typehandler;
