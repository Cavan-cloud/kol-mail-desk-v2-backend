package com.lovart.maildesk.common.typehandler;

import com.lovart.maildesk.common.enums.KolStatus;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Binds {@link KolStatus} to the PostgreSQL {@code kol_status} ENUM.
 */
@MappedTypes(KolStatus.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class KolStatusTypeHandler extends PgEnumTypeHandler<KolStatus> {

    public KolStatusTypeHandler() {
        super(KolStatus.class, "kol_status");
    }
}
