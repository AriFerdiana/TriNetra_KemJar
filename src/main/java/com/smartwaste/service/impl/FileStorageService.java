package com.smartwaste.service.impl;

import com.smartwaste.service.SecurityAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service untuk mengelola penyimpanan file upload dengan validasi keamanan berlapis.
 *
 * <p><b>Lapisan Keamanan:</b></p>
 * <ol>
 *   <li><b>Whitelist Ekstensi</b>: Hanya mengizinkan ekstensi file yang aman.</li>
 *   <li><b>Magic Bytes Check</b>: Membaca byte pertama file untuk memverifikasi
 *       tipe file sebenarnya, mencegah penyerang menyamar file berbahaya
 *       (misal: webshell.jsp yang diganti nama menjadi avatar.jpg).</li>
 *   <li><b>Path Traversal Prevention</b>: Mencegah nama file dengan "..".</li>
 *   <li><b>Random UUID Filename</b>: File disimpan dengan nama acak, mencegah
 *       prediksi/enumerasi nama file.</li>
 * </ol>
 */
@Service
public class FileStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileStorageService.class);

    /** Ekstensi file yang diizinkan (whitelist). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".csv"
    );

    /**
     * Magic bytes (tanda tangan file) untuk tipe file yang diizinkan.
     * Map dari prefix Magic Bytes hex → tipe MIME yang valid.
     */
    private static final Map<String, String> MAGIC_BYTES = Map.of(
        "FFD8FF",     "image/jpeg",   // JPEG/JPG
        "89504E47",   "image/png",    // PNG
        "47494638",   "image/gif",    // GIF
        "25504446",   "application/pdf", // PDF
        "52494646",   "image/webp"    // WebP (RIFF header)
    );

    private final Path fileStorageLocation;
    private final SecurityAuditService auditService;

    public FileStorageService(@Value("${app.upload.dir:uploads/}") String uploadDir,
                              SecurityAuditService auditService) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.auditService = auditService;
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create upload directory", ex);
        }
    }

    /**
     * Simpan file setelah melewati validasi keamanan berlapis.
     *
     * @param file          file yang di-upload
     * @param uploaderIp    IP user yang melakukan upload (untuk audit log)
     * @return path relatif file yang tersimpan, atau null jika gagal/ditolak
     */
    public String storeFile(MultipartFile file, String uploaderIp) {
        if (file == null || file.isEmpty()) return null;

        String originalFileName = StringUtils.cleanPath(
            file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );

        // 1. Cegah path traversal
        if (originalFileName.contains("..")) {
            auditService.logFileUploadBlocked(originalFileName, uploaderIp, "Path traversal attempt");
            throw new SecurityException("Nama file tidak valid: " + originalFileName);
        }

        // 2. Validasi ekstensi (whitelist)
        String extension = getExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            auditService.logFileUploadBlocked(originalFileName, uploaderIp,
                "Extension not allowed: " + extension);
            throw new SecurityException("Tipe file tidak diizinkan: " + extension +
                ". Hanya diizinkan: " + ALLOWED_EXTENSIONS);
        }

        try {
            // 3. Validasi Magic Bytes (cek isi file sesungguhnya)
            byte[] header = new byte[8];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4) {
                    auditService.logFileUploadBlocked(originalFileName, uploaderIp, "File too small");
                    throw new SecurityException("File terlalu kecil atau kosong");
                }
            }

            // CSV tidak punya magic bytes standar — skip magic bytes check untuk .csv
            if (!extension.equals(".csv") && !isValidMagicBytes(header)) {
                auditService.logFileUploadBlocked(originalFileName, uploaderIp,
                    "Magic bytes mismatch — file content does not match extension");
                throw new SecurityException("Konten file tidak sesuai dengan ekstensinya. Upload ditolak.");
            }

            // 4. Simpan dengan nama UUID acak (hindari prediksi nama file)
            String targetFileName = UUID.randomUUID() + extension;
            Path targetLocation = fileStorageLocation.resolve(targetFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("File berhasil di-upload: {} -> {}", originalFileName, targetFileName);
            return "/uploads/" + targetFileName;

        } catch (SecurityException e) {
            throw e;
        } catch (IOException ex) {
            log.error("Gagal menyimpan file: {}", originalFileName, ex);
            return null;
        }
    }

    /** Overload untuk backward-compatibility (tanpa IP). */
    public String storeFile(MultipartFile file) {
        return storeFile(file, "unknown");
    }

    // ==================== Private Helpers ====================

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0) ? filename.substring(i) : "";
    }

    /** Cek apakah magic bytes file cocok dengan salah satu tipe yang diizinkan. */
    private boolean isValidMagicBytes(byte[] header) {
        String hexHeader = bytesToHex(header).toUpperCase();
        for (String magic : MAGIC_BYTES.keySet()) {
            if (hexHeader.startsWith(magic)) {
                return true;
            }
        }
        return false;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 6); i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}

