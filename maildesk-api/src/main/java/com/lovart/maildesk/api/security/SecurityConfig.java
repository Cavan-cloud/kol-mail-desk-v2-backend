package com.lovart.maildesk.api.security;

import com.lovart.maildesk.infrastructure.session.SessionTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless REST + OAuth2 login. Auth strategy:
 * <ol>
 *   <li>Browser hits {@code /oauth2/authorization/google} → Google consent →
 *       Spring Security callback at {@code /login/oauth2/code/google}.</li>
 *   <li>{@link GoogleOAuthSuccessHandler} upserts the profile, AES-encrypts the
 *       tokens, mints a session token, sets {@code MAILDESK_SESSION} cookie,
 *       and 302s back to the web app.</li>
 *   <li>Subsequent API calls carry the cookie; the request is bound by
 *       {@link SessionCookieAuthenticationFilter} before authorization runs.</li>
 *   <li>Anything still anonymous on a protected path hits
 *       {@link RestAuthenticationEntryPoint} → 401 JSON (no redirect).</li>
 * </ol>
 * CSRF is disabled because all state-changing endpoints sit behind a SameSite
 * cookie + custom-header check pattern at the controller layer; cross-origin
 * form posts cannot reach them.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/v1/health",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus",
            "/error",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final RestAuthenticationEntryPoint authEntryPoint;
    private final GoogleOAuthSuccessHandler oauthSuccessHandler;
    private final SessionTokenService sessionTokens;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            RestAuthenticationEntryPoint authEntryPoint,
            GoogleOAuthSuccessHandler oauthSuccessHandler,
            SessionTokenService sessionTokens,
            @Value("${maildesk.cors.allowed-origins:http://localhost:3000}") String allowedOriginsCsv
    ) {
        this.authEntryPoint = authEntryPoint;
        this.oauthSuccessHandler = oauthSuccessHandler;
        this.sessionTokens = sessionTokens;
        this.allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrations) throws Exception {
        SessionCookieAuthenticationFilter cookieFilter = new SessionCookieAuthenticationFilter(sessionTokens);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // NOTE: do NOT force SessionCreationPolicy.STATELESS — Spring
                // Security's OAuth2 client stores the authorization request +
                // state in HttpSession between /oauth2/authorization and the
                // callback. STATELESS breaks the flow. Our app-level session is
                // separately tracked via the Redis-backed MAILDESK_SESSION
                // cookie.
                //
                // BUT: by default Spring Security ALSO persists the final
                // OAuth2AuthenticationToken into that same HttpSession
                // (SPRING_SECURITY_CONTEXT attribute) once oauth2Login succeeds.
                // SecurityContextHolderFilter then restores that raw Google
                // token from the JSESSIONID cookie on every later request,
                // BEFORE SessionCookieAuthenticationFilter runs — which only
                // binds MAILDESK_SESSION when the context is still empty. Net
                // effect: after logging in once, the browser carries both
                // JSESSIONID + MAILDESK_SESSION, the stale Google token wins,
                // and every request looks unauthenticated (401 loop back to
                // /login). NullSecurityContextRepository stops Spring Security
                // from ever writing/reading that HttpSession-backed context —
                // the authorization-request state above is unaffected since it
                // is tracked by a separate AuthorizationRequestRepository.
                .securityContext(ctx -> ctx.securityContextRepository(new NullSecurityContextRepository()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(cookieFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(ep -> ep.authorizationRequestResolver(
                                new GoogleAuthorizationRequestResolver(clientRegistrations,
                                        "/oauth2/authorization")))
                        .successHandler(oauthSuccessHandler)
                        .userInfoEndpoint(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
