package com.lovart.maildesk.common.typehandler;

import com.lovart.maildesk.common.enums.EmailDirection;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Binds {@link EmailDirection} to the PostgreSQL {@code email_direction} ENUM.
 */
@MappedTypes(EmailDirection.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class EmailDirectionTypeHandler extends PgEnumTypeHandler<EmailDirection> {

    public EmailDirectionTypeHandler() {
        super(EmailDirection.class, "email_direction");
    }
}
