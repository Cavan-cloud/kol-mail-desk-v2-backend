package com.lovart.maildesk.common.typehandler;

import com.lovart.maildesk.common.enums.ActionType;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Binds {@link ActionType} to the PostgreSQL {@code action_type} ENUM.
 */
@MappedTypes(ActionType.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class ActionTypeTypeHandler extends PgEnumTypeHandler<ActionType> {

    public ActionTypeTypeHandler() {
        super(ActionType.class, "action_type");
    }
}
