package com.lovart.maildesk.common.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base translator for {@code Java enum ⇄ PostgreSQL ENUM} round-trips.
 * <p>
 * PostgreSQL refuses implicit {@code text → enum} coercion in parameterised
 * statements, so writes are wrapped in a {@link PGobject} tagged with the enum
 * type name. Java {@code UPPER_CASE} constants are mapped to the DB
 * {@code lower_case} labels by default — subclasses may override
 * {@link #toDbValue(Enum)} / {@link #fromDbValue(String)} if a particular ENUM
 * needs custom casing.
 */
public abstract class PgEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> enumType;
    private final String pgTypeName;

    protected PgEnumTypeHandler(Class<E> enumType, String pgTypeName) {
        this.enumType = enumType;
        this.pgTypeName = pgTypeName;
    }

    @Override
    public final void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject obj = new PGobject();
        obj.setType(pgTypeName);
        obj.setValue(toDbValue(parameter));
        ps.setObject(i, obj);
    }

    @Override
    public final E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public final E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public final E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    protected String toDbValue(E value) {
        return value.name().toLowerCase();
    }

    protected E fromDbValue(String raw) {
        return Enum.valueOf(enumType, raw.toUpperCase());
    }

    private E parse(String raw) {
        return raw == null ? null : fromDbValue(raw);
    }
}
