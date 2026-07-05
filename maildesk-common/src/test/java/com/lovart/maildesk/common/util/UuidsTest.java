package com.lovart.maildesk.common.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidsTest {

    @Test
    void parseCompactHexFromMyBatisPlusAssignUuid() {
        UUID expected = UUID.fromString("9818f9cb-ecd6-e2d4-3e10-c8e761b585d9");
        assertThat(Uuids.parse("9818f9cbecd6e2d43e10c8e761b585d9")).isEqualTo(expected);
    }

    @Test
    void parseStandardUuidString() {
        UUID id = UUID.randomUUID();
        assertThat(Uuids.parse(id.toString())).isEqualTo(id);
        assertThat(Uuids.parse(id)).isEqualTo(id);
    }
}
