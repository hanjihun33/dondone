package com.workproofpay.backend.employer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class WorkerRegistrationCodeCryptoService {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public WorkerRegistrationCodeCryptoService(
            @Value("${worker.registration-code-encryption-key:${employer.signup-code-encryption-key:}}") String encodedKey
    ) {
        this.secretKey = decodeSecretKey(encodedKey);
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = generateIv();
            byte[] encryptedBytes = initCipher(Cipher.ENCRYPT_MODE, iv)
                    .doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return encodePayload(iv, encryptedBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("worker registration code encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            EncryptedPayload payload = decodePayload(encryptedText);
            byte[] plainBytes = initCipher(Cipher.DECRYPT_MODE, payload.iv())
                    .doFinal(payload.cipherText());
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("worker registration code decryption failed", e);
        }
    }

    private Cipher initCipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher;
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private String encodePayload(byte[] iv, byte[] encryptedBytes) {
        byte[] payload = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, payload, iv.length, encryptedBytes.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    private EncryptedPayload decodePayload(String encryptedText) {
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encryptedText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("worker registration code payload is not valid base64", e);
        }

        if (payload.length <= GCM_IV_BYTES) {
            throw new IllegalArgumentException("worker registration code payload is too short");
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] cipherText = new byte[payload.length - GCM_IV_BYTES];
        System.arraycopy(payload, 0, iv, 0, GCM_IV_BYTES);
        System.arraycopy(payload, GCM_IV_BYTES, cipherText, 0, cipherText.length);
        return new EncryptedPayload(iv, cipherText);
    }

    private SecretKeySpec decodeSecretKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("WORKER_REGISTRATION_CODE_ENCRYPTION_KEY is required");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("WORKER_REGISTRATION_CODE_ENCRYPTION_KEY must be valid base64", e);
        }

        if (!isSupportedAesKeyLength(keyBytes.length)) {
            throw new IllegalStateException("WORKER_REGISTRATION_CODE_ENCRYPTION_KEY must decode to 16, 24, or 32 bytes");
        }

        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    private boolean isSupportedAesKeyLength(int length) {
        return length == 16 || length == 24 || length == 32;
    }

    private record EncryptedPayload(byte[] iv, byte[] cipherText) {
    }
}
