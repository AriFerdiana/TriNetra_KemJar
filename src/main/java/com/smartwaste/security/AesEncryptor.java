package com.smartwaste.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter untuk enkripsi/dekripsi data sensitif di database.
 *
 * <p>Menggunakan algoritma <b>AES-256-GCM</b> (authenticated encryption) —
 * lebih aman dari AES-CBC karena mencegah tampering pada ciphertext.</p>
 *
 * <p>Konverter ini bekerja secara transparan:
 * <ul>
 *   <li>Saat menyimpan ke database ({@link #convertToDatabaseColumn}): plaintext → ciphertext (Base64)</li>
 *   <li>Saat membaca dari database ({@link #convertToEntityAttribute}): ciphertext → plaintext</li>
 * </ul>
 * </p>
 *
 * <p>Cukup tambahkan {@code @Convert(converter = AesEncryptor.class)} pada field entity.</p>
 */
@Component
@Converter
public class AesEncryptor implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptor.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    // Secret key diambil dari application.yml
    @Value("${app.security.aes-secret-key}")
    private String secretKey;

    /**
     * Mengenkripsi plaintext menjadi Base64-encoded ciphertext sebelum disimpan ke DB.
     * IV (Initialization Vector) di-generate secara acak setiap kali enkripsi untuk
     * memastikan ciphertext yang berbeda meski input sama.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), spec);

            byte[] encrypted = cipher.doFinal(attribute.getBytes());

            // Gabungkan IV + ciphertext, encode ke Base64 untuk disimpan sebagai String
            ByteBuffer bb = ByteBuffer.allocate(iv.length + encrypted.length);
            bb.put(iv);
            bb.put(encrypted);

            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            log.error("Gagal mengenkripsi data", e);
            throw new RuntimeException("Enkripsi data gagal", e);
        }
    }

    /**
     * Mendekripsi Base64-encoded ciphertext dari DB menjadi plaintext saat dibaca.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer bb = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            bb.get(iv);

            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            log.error("Gagal mendekripsi data dari database", e);
            return dbData; // Kembalikan data asli (mungkin belum terenkripsi / migrasi)
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        // Pastikan key tepat 32 bytes (256-bit) dengan padding/truncate
        byte[] keyBytes = new byte[32];
        byte[] rawKey = secretKey.getBytes();
        System.arraycopy(rawKey, 0, keyBytes, 0, Math.min(rawKey.length, keyBytes.length));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
