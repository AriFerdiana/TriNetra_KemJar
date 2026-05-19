package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entitas Collector (Petugas Pengumpul Sampah) — subclass dari {@link User}.
 *
 * <p><b>OOP Concept — Inheritance & Polymorphism:</b>
 * Mewarisi semua field dari {@link User} dan {@link BaseEntity}. Meng-override
 * {@link User#getRole()} untuk mengembalikan {@code "COLLECTOR"}.</p>
 *
 * <p>Dalam konteks proyek NetraDUMP, {@code Collector} bisa merepresentasikan
 * <b>robot pengumpul otomatis</b> maupun petugas manusia. Endpoint IoT di
 * {@code /api/v1/iot/dump} akan menggunakan {@code collectorId} dari device
 * yang terdaftar sebagai Collector untuk mencatat setoran otomatis.</p>
 *
 * <p><b>Relasi:</b></p>
 * <ul>
 *   <li><b>OneToMany</b> dengan {@link WasteDeposit} — collector mengkonfirmasi setoran warga.</li>
 * </ul>
 *
 * <p><b>Hak Akses Collector (RBAC):</b></p>
 * <ul>
 *   <li>Mengkonfirmasi setoran sampah (PENDING → CONFIRMED)</li>
 *   <li>Menolak setoran tidak valid (PENDING → REJECTED)</li>
 *   <li>Melihat daftar setoran di area tugasnya</li>
 *   <li>Update status ketersediaan</li>
 * </ul>
 */
@Getter
@Setter
@Entity
@Table(name = "collectors")
@DiscriminatorValue("COLLECTOR")
@PrimaryKeyJoinColumn(name = "user_id")
public class Collector extends User {

    public Collector() {
        super();
        this.confirmedDeposits = new ArrayList<>();
    }

    /**
     * Nomor kendaraan pengangkut sampah petugas (atau ID robot untuk IoT).
     * Contoh: "B 1234 XY" atau "ROBOT-NETRADUMP-001".
     */
    @Column(name = "vehicle_number", length = 30)
    private String vehicleNumber;

    /**
     * Area/wilayah penugasan petugas (contoh: "RT 01-05 Kelurahan Sukamaju").
     * Untuk robot IoT, ini bisa berupa nama lokasi bin/tempat sampah pintar.
     */
    @Column(name = "assigned_area", length = 200)
    private String assignedArea;

    /**
     * Status ketersediaan petugas/robot saat ini.
     * {@code true} = siap bertugas, {@code false} = sedang tidak bertugas.
     */
    @Column(name = "available", nullable = false)
    private boolean available = true;

    /**
     * Apakah collector ini adalah perangkat IoT (robot/smart bin).
     * Digunakan oleh {@code IoTController} untuk verifikasi.
     */
    @Column(name = "is_iot_device", nullable = false)
    private boolean iotDevice = false;

    /**
     * Device ID unik untuk perangkat IoT (misal: "NETRADUMP-001").
     * Null jika petugas manusia.
     */
    @Column(name = "iot_device_id", unique = true, length = 50)
    private String iotDeviceId;

    @Column(name = "max_capacity_kg")
    private Double maxCapacityKg = 500.0;

    @Column(name = "current_load_kg")
    private Double currentLoadKg = 0.0;

    // ==================== Relasi ====================

    /**
     * Daftar setoran sampah yang telah dikonfirmasi oleh collector ini.
     *
     * <p>Relasi {@code OneToMany} — satu collector bisa mengkonfirmasi banyak setoran.
     * Menggunakan {@code FetchType.LAZY} untuk efisiensi memori.</p>
     */
    @OneToMany(mappedBy = "collector", fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<WasteDeposit> confirmedDeposits = new ArrayList<>();

    /**
     * Konstruktor untuk membuat Collector (petugas manusia) baru.
     *
     * @param name         nama lengkap petugas
     * @param email        alamat email
     * @param password     password yang sudah di-hash BCrypt
     * @param phone        nomor telepon
     * @param vehicleNumber nomor kendaraan
     * @param assignedArea area penugasan
     */
    public Collector(String name, String email, String password,
                     String phone, String vehicleNumber, String assignedArea) {
        this.setName(name);
        this.setEmail(email);
        this.setPassword(password);
        this.setPhone(phone);
        this.vehicleNumber = vehicleNumber;
        this.assignedArea = assignedArea;
        this.iotDevice = false;
    }

    /**
     * Konstruktor khusus untuk membuat Collector sebagai perangkat IoT (robot).
     *
     * @param name         nama/label robot
     * @param email        email robot (untuk autentikasi API)
     * @param password     API secret robot
     * @param iotDeviceId  ID perangkat unik (misal: "NETRADUMP-001")
     * @param assignedArea lokasi penempatan smart bin/robot
     */
    public Collector(String name, String email, String password,
                     String iotDeviceId, String assignedArea, boolean isIotDevice) {
        this.setName(name);
        this.setEmail(email);
        this.setPassword(password);
        this.iotDeviceId = iotDeviceId;
        this.assignedArea = assignedArea;
        this.iotDevice = true;
    }

    /**
     * Override method abstract dari {@link User}.
     *
     * @return string "COLLECTOR"
     */
    @Override
    public String getRole() {
        return "COLLECTOR";
    }
    // ==================== Manual Getters/Setters ====================
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getAssignedArea() { return assignedArea; }
    public void setAssignedArea(String assignedArea) { this.assignedArea = assignedArea; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isIotDevice() { return iotDevice; }
    public void setIotDevice(boolean iotDevice) { this.iotDevice = iotDevice; }
    public String getIotDeviceId() { return iotDeviceId; }
    public void setIotDeviceId(String iotDeviceId) { this.iotDeviceId = iotDeviceId; }
    public Double getMaxCapacityKg() { return maxCapacityKg; }
    public void setMaxCapacityKg(Double maxCapacityKg) { this.maxCapacityKg = maxCapacityKg; }
    public Double getCurrentLoadKg() { return currentLoadKg; }
    public void setCurrentLoadKg(Double currentLoadKg) { this.currentLoadKg = currentLoadKg; }
    public List<WasteDeposit> getConfirmedDeposits() { return confirmedDeposits; }
    public void setConfirmedDeposits(List<WasteDeposit> confirmedDeposits) { this.confirmedDeposits = confirmedDeposits; }
}
