package com.smartwaste.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service terpusat untuk mencatat semua event keamanan aplikasi.
 *
 * <p>Log yang dihasilkan ditulis dengan format terstruktur yang dapat
 * dibaca dan di-parse oleh Wazuh Agent di server Ubuntu/Debian nanti.
 * Format: [TIMESTAMP] [EVENT_TYPE] IP=x.x.x.x | DETAIL</p>
 *
 * <p>Tipe event yang dicatat:</p>
 * <ul>
 *   <li>LOGIN_SUCCESS — Login berhasil</li>
 *   <li>LOGIN_FAILED — Login gagal (salah password/email)</li>
 *   <li>BRUTE_FORCE_DETECTED — Rate limit terlampaui dari satu IP</li>
 *   <li>FILE_UPLOAD_BLOCKED — Upload file berbahaya dicegah</li>
 *   <li>ACCESS_DENIED — Akses ke resource yang tidak diizinkan</li>
 *   <li>MFA_SUCCESS — Verifikasi OTP berhasil</li>
 *   <li>MFA_FAILED — Verifikasi OTP gagal</li>
 *   <li>XSS_ATTEMPT — Potensi percobaan XSS terdeteksi</li>
 * </ul>
 */
@Service
public class SecurityAuditService {

    // Logger ini ditulis ke file log terpisah (smartwaste-security.log)
    // dikonfigurasi via logback-spring.xml
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Catat event keamanan ke log audit.
     *
     * @param eventType tipe event (misal: "BRUTE_FORCE_DETECTED")
     * @param ip        alamat IP client
     * @param detail    detail tambahan mengenai event
     */
    public void logSecurityEvent(String eventType, String ip, String detail) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        // Format terstruktur yang mudah di-parse oleh Wazuh regex rule
        securityLog.warn("[SECURITY_EVENT] timestamp={} event={} ip={} detail=\"{}\"",
            timestamp, eventType, ip, detail);
    }

    /** Shortcut untuk mencatat login berhasil. */
    public void logLoginSuccess(String email, String ip) {
        logSecurityEvent("LOGIN_SUCCESS", ip, "User=" + email);
    }

    /** Shortcut untuk mencatat login gagal. */
    public void logLoginFailed(String email, String ip) {
        logSecurityEvent("LOGIN_FAILED", ip, "User=" + email + " | Reason=BadCredentials");
    }

    /** Shortcut untuk mencatat akses ditolak. */
    public void logAccessDenied(String path, String ip, String role) {
        logSecurityEvent("ACCESS_DENIED", ip, "Path=" + path + " | Role=" + role);
    }

    /** Shortcut untuk mencatat file upload yang dicegah. */
    public void logFileUploadBlocked(String filename, String ip, String reason) {
        logSecurityEvent("FILE_UPLOAD_BLOCKED", ip, "File=" + filename + " | Reason=" + reason);
    }

    /** Shortcut untuk mencatat event MFA. */
    public void logMfaEvent(String eventType, String email, String ip) {
        logSecurityEvent(eventType, ip, "User=" + email);
    }
}
