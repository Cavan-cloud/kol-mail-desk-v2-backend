package com.lovart.maildesk.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AES-256-GCM token encryption. No Docker required; runs fast in
 * surefire alongside the rest of the unit suite.
 */
class TokenEncryptionServiceTest {

    private static final String KEY_A = Base64.getEncoder().encodeToString(filled((byte) 0x11, 32));
    private static final String KEY_B = Base64.getEncoder().encodeToString(filled((byte) 0x22, 32));

    @Test
    void roundTripPlaintext() {
        TokenEncryptionService svc = new TokenEncryptionService(KEY_A);
        String plain = "{\"access_token\":\"ya29.example\",\"refresh_token\":\"1//refresh\"}";
        byte[] sealed = svc.encrypt(plain);
        assertThat(svc.decrypt(sealed)).isEqualTo(plain);
    }

    @Test
    void differentNoncesProduceDifferentCiphertext() {
        TokenEncryptionService svc = new TokenEncryptionService(KEY_A);
        byte[] a = svc.encrypt("payload");
        byte[] b = svc.encrypt("payload");
        // IV is random per call → ciphertext (incl IV prefix) must differ.
        assertThat(Arrays.equals(a, b)).isFalse();
        assertThat(svc.decrypt(a)).isEqualTo("payload");
        assertThat(svc.decrypt(b)).isEqualTo("payload");
    }

    @Test
    void wrongKeyFailsDecryption() {
        TokenEncryptionService a = new TokenEncryptionService(KEY_A);
        TokenEncryptionService b = new TokenEncryptionService(KEY_B);
        byte[] sealed = a.encrypt("secret");
        assertThatThrownBy(() -> b.decrypt(sealed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("decryption failed");
    }

    @Test
    void tamperedCiphertextFailsAuthTag() {
        TokenEncryptionService svc = new TokenEncryptionService(KEY_A);
        byte[] sealed = svc.encrypt("integrity-matters");
        // Flip a bit in the ciphertext body (skip the 12-byte IV).
        sealed[15] ^= 0x01;
        assertThatThrownBy(() -> svc.decrypt(sealed))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingKeyRefusesToBoot() {
        assertThatThrownBy(() -> new TokenEncryptionService(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void wrongLengthKeyRefusesToBoot() {
        String tooShort = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new TokenEncryptionService(tooShort))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void nonBase64KeyRefusesToBoot() {
        assertThatThrownBy(() -> new TokenEncryptionService("not_base64!@#$"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void truncatedSealedBytesAreRejected() {
        TokenEncryptionService svc = new TokenEncryptionService(KEY_A);
        assertThatThrownBy(() -> svc.decrypt(new byte[5]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] filled(byte v, int len) {
        byte[] a = new byte[len];
        Arrays.fill(a, v);
        return a;
    }
}
