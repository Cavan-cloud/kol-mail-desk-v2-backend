package com.lovart.maildesk.infrastructure.crypto;

import com.lovart.maildesk.domain.crypto.TokenEncryptionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * AES-256-GCM authenticated encryption for OAuth tokens persisted in
 * {@code integration_credentials.encrypted_payload}.
 * <p>
 * Wire format (raw {@code byte[]} stored in PG {@code BYTEA}):
 * <pre>
 *   [ iv (12 bytes) ][ ciphertext (n bytes) ][ auth tag (16 bytes) ]
 * </pre>
 * The cipher's {@link Cipher#doFinal(byte[])} appends the GCM tag to the
 * ciphertext, so we only need to prepend the IV manually. Tampering with any
 * byte fails the tag check and throws on {@link #decrypt(byte[])} — callers
 * MUST treat the failure as a hard error (rotate creds, do not surface raw
 * exception to clients).
 * <p>
 * Master key is provided as a base64-encoded 32-byte value via
 * {@code TOKEN_ENCRYPTION_KEY} (see {@code .env.example}). On startup we
 * fail-fast if the key is missing or the wrong length so production never boots
 * with a weak default.
 */
@Component
public class TokenEncryptionService implements TokenEncryptionPort {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;       // AES-256
    private static final int IV_BYTES = 12;        // GCM standard
    private static final int TAG_BITS = 128;       // GCM auth tag

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public TokenEncryptionService(@Value("${maildesk.token-encryption-key:${TOKEN_ENCRYPTION_KEY:}}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "TOKEN_ENCRYPTION_KEY is required for AES-256-GCM token encryption. " +
                            "Generate one via `openssl rand -base64 32` and set it in your .env "
                            + "(see kol-mail-desk-v2-docs/specs/SETUP.md §4.1).");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must be base64-encoded.", e);
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "TOKEN_ENCRYPTION_KEY must decode to exactly " + KEY_BYTES + " bytes (AES-256); got "
                            + raw.length + ". Regenerate via `openssl rand -base64 32`.");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /**
     * Encrypt {@code plaintext} (UTF-8) and return raw bytes ready for the DB column.
     */
    public byte[] encrypt(String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherAndTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherAndTag.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherAndTag, 0, out, iv.length, cipherAndTag.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-256-GCM encryption failed", e);
        }
    }

    /**
     * Decrypt bytes previously produced by {@link #encrypt(String)}. Throws if
     * the input is too short, tampered, or encrypted under a different key.
     */
    public String decrypt(byte[] sealed) {
        Objects.requireNonNull(sealed, "sealed");
        if (sealed.length <= IV_BYTES) {
            throw new IllegalArgumentException("Sealed payload too short for IV + ciphertext.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(sealed, 0, iv, 0, IV_BYTES);
            byte[] cipherAndTag = new byte[sealed.length - IV_BYTES];
            System.arraycopy(sealed, IV_BYTES, cipherAndTag, 0, cipherAndTag.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherAndTag);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-256-GCM decryption failed (tamper or wrong key)", e);
        }
    }
}
