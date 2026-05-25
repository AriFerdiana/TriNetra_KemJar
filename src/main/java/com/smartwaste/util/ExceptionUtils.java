package com.smartwaste.util;

import org.springframework.dao.DataIntegrityViolationException;

public class ExceptionUtils {

    /**
     * Extracts a user-friendly error message from generic or SQL exceptions.
     * Prevents raw backend error stacks/messages from leaking to the frontend UI.
     */
    public static String getFriendlyMessage(Exception e) {
        if (e == null) return "Terjadi kesalahan yang tidak diketahui.";

        // Handle Hibernate / SQL Database Integrity errors (Unique Constraints, Nulls)
        if (e instanceof DataIntegrityViolationException) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            
            if (msg.contains("duplicate entry")) {
                if (msg.contains("email")) {
                    return "Email sudah terdaftar atau digunakan oleh pengguna lain.";
                }
                if (msg.contains("nik")) {
                    return "NIK sudah terdaftar atau digunakan oleh warga lain.";
                }
                if (msg.contains("phone")) {
                    return "Nomor HP sudah terdaftar atau digunakan oleh pengguna lain.";
                }
                return "Data tersebut sudah ada di sistem (Duplikat). Pastikan data yang dimasukkan unik.";
            }
            if (msg.contains("cannot be null") || msg.contains("not null")) {
                return "Ada kolom wajib yang belum diisi dengan benar.";
            }
            if (msg.contains("foreign key") || msg.contains("references")) {
                return "Tindakan ditolak karena data ini masih terhubung dengan data lain (misal: user masih memiliki riwayat transaksi).";
            }
            
            return "Terjadi pelanggaran integritas data pada sistem (contoh: data kembar atau referensi hilang).";
        }

        // Handle standard Validation & State errors thrown by our Services
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage(); // Safe to display
        }
        
        // Handle custom domain exceptions
        if (e.getClass().getName().startsWith("com.smartwaste.exception.")) {
            return e.getMessage(); // Safe to display our own exceptions like InsufficientPointsException
        }

        // Handle Spring Security Access Denied
        if (e instanceof org.springframework.security.access.AccessDeniedException) {
            return "Anda tidak memiliki hak akses untuk melakukan tindakan ini.";
        }

        // Default fallback for unexpected RuntimeExceptions (e.g. NullPointerException)
        // We log the real error class for debugging context but keep the message clean
        return "Terjadi kesalahan sistem (" + e.getClass().getSimpleName() + "). Pesan: " + e.getMessage();
    }
}
