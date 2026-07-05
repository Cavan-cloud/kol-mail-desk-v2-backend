package com.lovart.maildesk.application.credential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovart.maildesk.domain.credential.GoogleAccessToken;
import com.lovart.maildesk.domain.credential.entity.IntegrationCredentialDO;
import com.lovart.maildesk.domain.credential.mapper.IntegrationCredentialMapper;
import com.lovart.maildesk.domain.crypto.TokenEncryptionPort;
import com.lovart.maildesk.integration.gmail.GmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleCredentialServiceTest {

    @Mock
    private IntegrationCredentialMapper credentials;

    @Mock
    private TokenEncryptionPort encryption;

    private GoogleCredentialService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void resolveAccessToken_returnsCachedTokenWhenNotExpired() throws Exception {
        GmailProperties props = new GmailProperties("client-id", "client-secret", java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(5), 1);
        service = new GoogleCredentialService(credentials, encryption, props, new ObjectMapper(), new RestTemplate());

        String json = """
                {"access_token":"cached-token","refresh_token":"refresh","expires_at":"%s"}
                """.formatted(Instant.now().plusSeconds(3600));
        IntegrationCredentialDO row = row(json);
        when(credentials.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row);

        Optional<GoogleAccessToken> token = service.resolveAccessToken(userId);

        assertThat(token).contains(new GoogleAccessToken("cached-token"));
    }

    @Test
    void resolveAccessToken_refreshesWhenExpired() throws Exception {
        GmailProperties props = new GmailProperties("client-id", "client-secret", java.time.Duration.ofSeconds(5),
                java.time.Duration.ofSeconds(5), 1);
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        service = new GoogleCredentialService(credentials, encryption, props, new ObjectMapper(), restTemplate);

        String json = """
                {"access_token":"old-token","refresh_token":"refresh","expires_at":"%s"}
                """.formatted(Instant.now().minusSeconds(60));
        IntegrationCredentialDO row = row(json);
        when(credentials.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row);
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"new-token","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

        Optional<GoogleAccessToken> token = service.resolveAccessToken(userId);

        assertThat(token).contains(new GoogleAccessToken("new-token"));
        verify(credentials).updateById(any(IntegrationCredentialDO.class));
        server.verify();
    }

    private IntegrationCredentialDO row(String json) throws Exception {
        IntegrationCredentialDO row = new IntegrationCredentialDO();
        row.setId(UUID.randomUUID());
        row.setUserId(userId);
        row.setType("google");
        when(encryption.decrypt(any())).thenReturn(json);
        row.setEncryptedPayload(json.getBytes(StandardCharsets.UTF_8));
        return row;
    }
}
