package com.lovart.maildesk.domain.credential;

/**
 * Valid OAuth access token for a user's Google integration.
 */
public record GoogleAccessToken(String accessToken) {
}
