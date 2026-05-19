package com.smartwaste.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "admin_logs")
public class AdminLog extends BaseEntity {

    @Column(nullable = false)
    private String adminEmail;

    @Column(nullable = false)
    private String action; // e.g., "DELETE_CATEGORY", "APPROVE_REDEMPTION"

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String ipAddress;
}
