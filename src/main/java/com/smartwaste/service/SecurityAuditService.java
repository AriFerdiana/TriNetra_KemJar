package com.smartwaste.service;

import com.smartwaste.entity.SecurityLog;
import com.smartwaste.repository.SecurityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service terpusat untuk mencatat semua event keamanan aplikasi.
 *
 * <p>Log yang dihasilkan ditulis ke DUA tujuan:</p>
 * <ol>
 *   <li><b>File Text (smartwaste-security.log)</b>: Dibaca oleh agen Wazuh di Ubuntu.</li>
 *   <li><b>Database MySQL (tabel security_logs)</b>: Dibaca oleh AI Dashboard internal kita.</li>
 * </ol>
 */
@Service
public class SecurityAuditService {

    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SecurityLogRepository securityLogRepository;

    @Autowired
    public SecurityAuditService(SecurityLogRepository securityLogRepository) {
        this.securityLogRepository = securityLogRepository;
    }

    /**
     * Catat event keamanan ke log file dan database.
     *
     * @param eventType tipe event (misal: "BRUTE_FORCE_DETECTED")
     * @param ip        alamat IP client
     * @param detail    detail tambahan mengenai event
     */
    public void logSecurityEvent(String eventType, String ip, String detail) {
        // 1. Catat ke file untuk Wazuh
        String timestamp = LocalDateTime.now().format(FORMATTER);
        securityLog.warn("[SECURITY_EVENT] timestamp={} event={} ip={} detail=\"{}\"",
            timestamp, eventType, ip, detail);

        // 2. Simpan ke database untuk Dashboard & AI Mistral
        try {
            SecurityLog dbLog = SecurityLog.builder()
                    .eventType(eventType)
                    .ipAddress(ip)
                    .detail(detail)
                    .build();
            securityLogRepository.save(dbLog);
        } catch (Exception e) {
            securityLog.error("Gagal menyimpan Security Log ke database: {}", e.getMessage());
        }
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
