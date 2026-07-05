package com.lovart.maildesk.domain.crypto;

/**
 * AES-256-GCM seal/open for integration credential payloads.
 */
public interface TokenEncryptionPort {

    byte[] encrypt(String plaintext);

    String decrypt(byte[] sealed);
}
