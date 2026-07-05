package com.lovart.maildesk.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Replaces Spring Security's default redirect-to-login flow with a JSON 401 so
 * the SPA (Next.js) can react cleanly. Public oauth2 / health / actuator paths
 * are permitted at the {@link SecurityConfig} layer and never reach here.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public RestAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // response.getWriter() defaults to ISO-8859-1 unless the character
        // encoding is set explicitly, which mangles the Chinese message into
        // "?" replacement characters. setCharacterEncoding MUST be called
        // before setContentType/getWriter().
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError("AUTH_REQUIRED", "请先登录");
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
