package com.smartwaste.entity;

import com.smartwaste.entity.enums.DepositStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entitas WasteDeposit — Transaksi Setoran Sampah.
 *
 * <p>Mencatat setiap aksi warga saat menyetorkan sampah ke petugas atau robot IoT.
 * Menyimpan data berat, kategori, poin yang diperoleh, dan bukti foto.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "waste_deposits", 
       indexes = {
           @Index(name = "idx_deposit_citizen", columnList = "citizen_id"),
           @Index(name = "idx_deposit_status", columnList = "status"),
           @Index(name = "idx_deposit_date", columnList = "created_at")
       })
public class WasteDeposit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id")
    private Collector collector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private WasteCategory category;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Column(name = "points_earned")
    private double pointsEarned = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DepositStatus status = DepositStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** URL foto sampah saat disetorkan (oleh warga) */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** URL foto bukti penjemputan (oleh petugas) */
    @Column(name = "pickup_proof_url", length = 500)
    private String pickupProofUrl;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** Apakah setoran berasal dari perangkat IoT */
    @Column(name = "from_iot", nullable = false)
    private boolean fromIoT = false;

    /** ID Perangkat IoT jika fromIoT = true */
    @Column(name = "iot_device_id", length = 50)
    private String iotDeviceId;

    /** Lokasi GPS/Nama Lokasi (khusus IoT/Mobile) */
    @Column(name = "location", length = 200)
    private String location;

    // ==================== Constructors ====================

    public WasteDeposit(Citizen citizen, WasteCategory category, double weightKg, String notes) {
        this.citizen = citizen;
        this.category = category;
        this.weightKg = weightKg;
        this.notes = notes;
        this.status = DepositStatus.PENDING;
        this.fromIoT = false;
    }

    public WasteDeposit(Citizen citizen, WasteCategory category, double weightKg,
                        String iotDeviceId, String location) {
        this.citizen = citizen;
        this.category = category;
        this.weightKg = weightKg;
        this.iotDeviceId = iotDeviceId;
        this.location = location;
        this.status = DepositStatus.PENDING;
        this.fromIoT = true;
    }

    // ==================== Business Logic Methods ====================

    public void confirm(Collector collector, double pointsEarned) {
        confirm(collector, pointsEarned, null);
    }

    public void confirm(Collector collector, double pointsEarned, String pickupProofUrl) {
        this.collector = collector;
        this.pointsEarned = pointsEarned;
        this.status = DepositStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        if (pickupProofUrl != null) {
            this.pickupProofUrl = pickupProofUrl;
        }
    }

    public void reject(Collector collector, String reason) {
        this.collector = collector;
        this.status = DepositStatus.REJECTED;
        this.notes = (this.notes != null ? this.notes + " | " : "") + "DITOLAK: " + reason;
        this.confirmedAt = LocalDateTime.now();
    }

    // ==================== Manual Getters/Setters ====================
    public Citizen getCitizen() {
        try {
            if (this.citizen != null) {
                this.citizen.getName();
            }
            return this.citizen;
        } catch (jakarta.persistence.EntityNotFoundException e) {
            Citizen dummy = new Citizen();
            dummy.setName("Warga Dihapus");
            return dummy;
        }
    }
    public void setCitizen(Citizen citizen) { this.citizen = citizen; }
    public Collector getCollector() {
        try {
            if (this.collector != null) {
                this.collector.getName();
            }
            return this.collector;
        } catch (jakarta.persistence.EntityNotFoundException e) {
            Collector dummy = new Collector();
            dummy.setName("Petugas Dihapus");
            return dummy;
        }
    }
    public void setCollector(Collector collector) { this.collector = collector; }
    public WasteCategory getCategory() { return category; }
    public void setCategory(WasteCategory category) { this.category = category; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public double getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(double pointsEarned) { this.pointsEarned = pointsEarned; }
    public DepositStatus getStatus() { return status; }
    public void setStatus(DepositStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPickupProofUrl() { return pickupProofUrl; }
    public void setPickupProofUrl(String pickupProofUrl) { this.pickupProofUrl = pickupProofUrl; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public boolean isFromIoT() { return fromIoT; }
    public void setFromIoT(boolean fromIoT) { this.fromIoT = fromIoT; }
    public String getIotDeviceId() { return iotDeviceId; }
    public void setIotDeviceId(String iotDeviceId) { this.iotDeviceId = iotDeviceId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
