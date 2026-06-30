package com.lovart.maildesk.api.security;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Wire-format error body. Mirrors the {@code ApiError} schema in
 * {@code api-contract-v1.yaml}. Kept package-private to the security layer here
 * since the only producer in P1-T04 is {@link RestAuthenticationEntryPoint}; a
 * shared DTO will move to {@code common.dto} once write-paths land in P5.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Map<String, Object> details) {

    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
