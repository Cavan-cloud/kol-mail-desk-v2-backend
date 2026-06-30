package com.lovart.maildesk.api.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lovart.maildesk.api.security.SessionPrincipal;
import com.lovart.maildesk.domain.profile.entity.ProfileDO;
import com.lovart.maildesk.domain.profile.mapper.ProfileMapper;
import com.lovart.maildesk.infrastructure.session.SessionTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Auth surface for the SPA. {@code /me} + {@code /auth/logout} match the
 * {@code api-contract-v1.yaml} schema; {@code /gmail/authorize} is a thin
 * redirect to the OAuth2 entry with {@code prompt=consent} so users can
 * re-authorize Gmail without a logout.
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final ProfileMapper profiles;
    private final SessionTokenService sessionTokens;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
            ProfileMapper profiles,
            SessionTokenService sessionTokens,
            @Value("${maildesk.session.cookie-secure:false}") boolean cookieSecure,
            @Value("${maildesk.session.cookie-same-site:Lax}") String cookieSameSite
    ) {
        this.profiles = profiles;
        this.sessionTokens = sessionTokens;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        ProfileDO row = profiles.selectById(principal.userId());
        if (row == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(MeResponse.from(row));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal SessionPrincipal principal,
                                       HttpServletResponse response) {
        if (principal != null) {
            sessionTokens.invalidate(principal.sessionToken());
        }
        response.addHeader("Set-Cookie", clearCookie());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/gmail/authorize")
    public RedirectView reauthorizeGmail() {
        // Spring Security's authorization endpoint; our resolver already injects
        // prompt=consent + access_type=offline so this works as "re-consent gmail".
        return new RedirectView("/oauth2/authorization/google");
    }

    private String clearCookie() {
        StringBuilder sb = new StringBuilder();
        sb.append(SessionTokenService.COOKIE_NAME).append("=");
        sb.append("; Path=/");
        sb.append("; Max-Age=0");
        sb.append("; HttpOnly");
        sb.append("; SameSite=").append(cookieSameSite);
        if (cookieSecure) sb.append("; Secure");
        return sb.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MeResponse(
            UUID id,
            UUID tenantId,
            String displayName,
            String email,
            String role,
            String status,
            UUID mentorUserId,
            String feishuOperatorName,
            OffsetDateTime approvedAt
    ) {
        static MeResponse from(ProfileDO p) {
            return new MeResponse(
                    p.getId(),
                    p.getTenantId(),
                    p.getDisplayName(),
                    p.getEmail(),
                    p.getRole(),
                    p.getStatus(),
                    p.getMentorUserId(),
                    p.getFeishuOperatorName(),
                    p.getApprovedAt()
            );
        }
    }
}
