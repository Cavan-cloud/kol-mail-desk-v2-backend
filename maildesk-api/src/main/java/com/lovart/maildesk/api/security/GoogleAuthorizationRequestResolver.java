package com.lovart.maildesk.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Forces Google to return a refresh_token on every consent (otherwise it only
 * issues one on the very first consent and never again — a footgun for any user
 * who has previously authorized the app). Achieved via two extra params on the
 * authorization URL:
 * <ul>
 *   <li>{@code access_type=offline} — request a refresh_token</li>
 *   <li>{@code prompt=consent}      — re-show the consent screen so Google
 *       actually attaches a refresh_token to the response</li>
 * </ul>
 */
public class GoogleAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public GoogleAuthorizationRequestResolver(ClientRegistrationRepository repo,
                                              String authorizationRequestBaseUri) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req) {
        if (req == null) return null;
        Map<String, Object> additional = new HashMap<>(req.getAdditionalParameters());
        additional.putIfAbsent("access_type", "offline");
        additional.putIfAbsent("prompt", "consent");
        return OAuth2AuthorizationRequest.from(req).additionalParameters(additional).build();
    }
}
