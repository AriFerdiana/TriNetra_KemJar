package com.smartwaste.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entitas untuk menyimpan log keamanan (Application Layer Security).
 * Menyimpan data seperti IP Address, jenis kejadian (Brute Force, Bypass),
 * dan detail tambahan untuk dianalisis oleh AI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "security_logs")
public class SecurityLog extends BaseEntity {

    @Column(nullable = false)
    private String eventType; // Contoh: BRUTE_FORCE_DETECTED, FILE_UPLOAD_BLOCKED

    @Column(nullable = false)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String detail;
}
