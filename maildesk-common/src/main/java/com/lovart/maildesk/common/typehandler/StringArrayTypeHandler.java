package com.lovart.maildesk.common.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Translates {@code List<String>} ⇄ PostgreSQL {@code text[]}.
 * <p>
 * Because Java generics are erased at runtime, MyBatis cannot infer the element
 * type from a bare {@code List} field. Attach this handler via
 * {@code @TableField(typeHandler = StringArrayTypeHandler.class)} together with
 * {@code @TableName(autoResultMap = true)} on entities that own a {@code text[]}
 * column.
 */
@MappedTypes(List.class)
@MappedJdbcTypes(value = JdbcType.ARRAY, includeNullJdbcType = true)
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    private static final String PG_TEXT = "text";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        Array array = ps.getConnection().createArrayOf(PG_TEXT, parameter.toArray(new String[0]));
        ps.setArray(i, array);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return Collections.emptyList();
        }
        Object raw = array.getArray();
        if (!(raw instanceof Object[] elements)) {
            return Collections.emptyList();
        }
        String[] copy = new String[elements.length];
        for (int idx = 0; idx < elements.length; idx++) {
            copy[idx] = elements[idx] == null ? null : elements[idx].toString();
        }
        return List.of(copy);
    }
}
