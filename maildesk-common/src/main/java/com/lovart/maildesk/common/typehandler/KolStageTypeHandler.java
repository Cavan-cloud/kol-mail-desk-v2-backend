package com.lovart.maildesk.common.typehandler;

import com.lovart.maildesk.common.enums.KolStage;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Binds {@link KolStage} to the PostgreSQL {@code kol_stage} ENUM.
 */
@MappedTypes(KolStage.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class KolStageTypeHandler extends PgEnumTypeHandler<KolStage> {

    public KolStageTypeHandler() {
        super(KolStage.class, "kol_stage");
    }
}
