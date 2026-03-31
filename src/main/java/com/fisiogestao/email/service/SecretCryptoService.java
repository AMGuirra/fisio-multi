package com.fisiogestao.email.service;

import com.fisiogestao.shared.web.BusinessException;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SecretCryptoService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String configuredKey;

    public SecretCryptoService(@Value("${app.security.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey == null ? "" : configuredKey.trim();
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new BusinessException("Informe o segredo para criptografia.");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível proteger o segredo informado.");
        }
    }

    public String decrypt(@Nullable String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return "";
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível ler o segredo armazenado.");
        }
    }

    private SecretKeySpec secretKey() {
        if (configuredKey.isBlank()) {
            throw new BusinessException(
                    "Defina APP_ENCRYPTION_KEY para proteger as credenciais de e-mail antes de salvar a configuração SMTP."
            );
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível inicializar a chave de criptografia.");
        }
    }
}
