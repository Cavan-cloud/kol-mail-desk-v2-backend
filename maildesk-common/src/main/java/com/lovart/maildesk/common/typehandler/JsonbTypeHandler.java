package com.lovart.maildesk.common.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Translates Jackson {@link JsonNode} ⇄ PostgreSQL {@code jsonb}.
 * <p>
 * Mapped to a generic Jackson type (rather than {@code Map<String,Object>}) so the
 * handler can store any JSONB shape — object, array, scalar — without forcing
 * callers to commit to a structural type.
 */
@MappedTypes(JsonNode.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class JsonbTypeHandler extends BaseTypeHandler<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PG_JSONB = "jsonb";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject obj = new PGobject();
        obj.setType(PG_JSONB);
        try {
            obj.setValue(MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialise JSONB value", e);
        }
        ps.setObject(i, obj);
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private JsonNode parse(String raw) throws SQLException {
        if (raw == null) {
            return null;
        }
        if (raw.isEmpty()) {
            return JsonNodeFactory.instance.objectNode();
        }
        try {
            return MAPPER.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to parse JSONB value: " + raw, e);
        }
    }
}
