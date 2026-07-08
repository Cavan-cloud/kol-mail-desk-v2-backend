package com.lovart.maildesk.application.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardDetailFilterTest {

    @Test
    void normalizeDetail_acceptsDrillModes() {
        assertThat(BoardDetailFilter.normalizeDetail("kols")).isEqualTo("kols");
        assertThat(BoardDetailFilter.normalizeDetail("unreplied")).isEqualTo("unreplied");
        assertThat(BoardDetailFilter.normalizeDetail("unread")).isEqualTo("unread");
        assertThat(BoardDetailFilter.normalizeDetail("overview")).isNull();
        assertThat(BoardDetailFilter.normalizeDetail(null)).isNull();
    }
}
