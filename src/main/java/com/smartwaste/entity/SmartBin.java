package com.smartwaste.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "smart_bins")
public class SmartBin extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String deviceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    /** Fill level in percentage (0 - 100) */
    @Column(nullable = false)
    private Integer fillLevel = 0;

    /** Status: "Aktif", "Penuh", "Offline", "Maintenance" */
    @Column(nullable = false)
    private String status = "Aktif";

    @Column
    private String color = "#10b981"; // Default Emerald

    public String getLocation() {
        return latitude + ", " + longitude;
    }

    public boolean isActive() {
        return !"Offline".equalsIgnoreCase(status);
    }

}
