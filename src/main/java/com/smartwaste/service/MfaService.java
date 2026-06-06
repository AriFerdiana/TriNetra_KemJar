package com.smartwaste.service;

import com.smartwaste.entity.User;
import com.smartwaste.repository.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

/**
 * Service untuk mengelola fitur Multi-Factor Authentication (MFA/2FA).
 *
 * <p>Menggunakan protokol TOTP (Time-based One-Time Password) yang kompatibel
 * dengan Google Authenticator, Authy, dan aplikasi authenticator lainnya.</p>
 *
 * <p>Alur MFA:</p>
 * <ol>
 *   <li>User mengaktifkan MFA → generate secret + tampilkan QR code</li>
 *   <li>User scan QR code dengan Google Authenticator</li>
 *   <li>User konfirmasi dengan kode 6-digit dari HP</li>
 *   <li>MFA diaktifkan di akun, secret disimpan terenkripsi</li>
 *   <li>Setiap login berikutnya, setelah password benar → diminta kode OTP</li>
 * </ol>
 */
@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);

    private static final String ISSUER = "SmartWaste TriNetra";
    private static final int DIGITS = 6;
    private static final int PERIOD = 30; // seconds

    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator;

    public MfaService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.secretGenerator = new DefaultSecretGenerator(32);
    }

    /**
     * Generate secret key baru untuk user dan kembalikan QR code sebagai Base64 PNG.
     *
     * @param email email user yang akan mengaktifkan MFA
     * @return data URI berisi QR code image (base64 PNG) untuk ditampilkan di HTML
     */
    public String generateQrCodeDataUri(String email) throws QrGenerationException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        // Generate secret baru jika belum ada
        String secret = user.getMfaSecret();
        if (secret == null || secret.isEmpty()) {
            secret = secretGenerator.generate();
            user.setMfaSecret(secret);
            userRepository.save(user);
        }

        // Build QR code data
        QrData qrData = new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer(ISSUER)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(DIGITS)
            .period(PERIOD)
            .build();

        // Generate QR code image
        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] qrCodeImage = qrGenerator.generate(qrData);

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeImage);
    }

    /**
     * Verifikasi kode OTP 6-digit yang dimasukkan user.
     *
     * @param email email user
     * @param code  kode 6-digit dari aplikasi authenticator
     * @return true jika kode valid, false jika tidak
     */
    public boolean verifyCode(String email, String code) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (user.getMfaSecret() == null) {
            return false;
        }

        return verifyTotpCode(user.getMfaSecret(), code);
    }

    /**
     * Aktifkan MFA untuk user setelah verifikasi pertama berhasil.
     *
     * @param email email user
     * @param code  kode OTP untuk konfirmasi aktivasi
     * @return true jika berhasil diaktifkan
     */
    @Transactional
    public boolean enableMfa(String email, String code) {
        if (!verifyCode(email, code)) {
            log.warn("MFA activation failed for user {}: invalid OTP", email);
            return false;
        }
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("MFA enabled successfully for user: {}", email);
        return true;
    }

    /**
     * Nonaktifkan MFA untuk user.
     */
    @Transactional
    public void disableMfa(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        log.info("MFA disabled for user: {}", email);
    }

    /** Cek apakah user memiliki MFA aktif. */
    public boolean isMfaEnabled(String email) {
        return userRepository.findByEmail(email)
            .map(User::isMfaEnabled)
            .orElse(false);
    }

    // ==================== Private Helper ====================

    private boolean verifyTotpCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        // Toleransi 3 periode (90 detik) ke depan/belakang untuk mengatasi drift waktu di local environment
        verifier.setAllowedTimePeriodDiscrepancy(3);
        return verifier.isValidCode(secret, code);
    }
}
