package com.lovart.maildesk.common.typehandler;

import com.lovart.maildesk.common.enums.Platform;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Binds {@link Platform} to the PostgreSQL {@code platform} ENUM.
 */
@MappedTypes(Platform.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class PlatformTypeHandler extends PgEnumTypeHandler<Platform> {

    public PlatformTypeHandler() {
        super(Platform.class, "platform");
    }
}
