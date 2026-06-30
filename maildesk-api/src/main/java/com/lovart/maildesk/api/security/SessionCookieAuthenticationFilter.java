package com.lovart.maildesk.api.security;

import com.lovart.maildesk.common.context.TenantContext;
import com.lovart.maildesk.common.context.UserContext;
import com.lovart.maildesk.infrastructure.session.SessionInfo;
import com.lovart.maildesk.infrastructure.session.SessionTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads the {@code MAILDESK_SESSION} cookie, looks the session up in Redis, and
 * binds an {@link Authentication} (plus {@link TenantContext} /
 * {@link UserContext}) for the duration of the request. Missing / unknown
 * cookies pass through; downstream {@link RestAuthenticationEntryPoint} will
 * 401 if the resource requires auth.
 */
public class SessionCookieAuthenticationFilter extends OncePerRequestFilter {

    private final SessionTokenService sessionTokens;

    public SessionCookieAuthenticationFilter(SessionTokenService sessionTokens) {
        this.sessionTokens = sessionTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Optional<String> token = readCookie(request);
        if (token.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            sessionTokens.find(token.get()).ifPresent(info -> bindAuth(info));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void bindAuth(SessionInfo info) {
        SessionPrincipal principal = SessionPrincipal.fromSession(info);
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + info.role().toUpperCase()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        if (info.tenantId() != null) TenantContext.setTenantId(info.tenantId());
        if (info.userId() != null) UserContext.setUserId(info.userId());
    }

    private Optional<String> readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie c : cookies) {
            if (SessionTokenService.COOKIE_NAME.equals(c.getName())) {
                return Optional.ofNullable(c.getValue());
            }
        }
        return Optional.empty();
    }
}
