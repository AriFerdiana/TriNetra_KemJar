package com.smartwaste.entity;

import com.smartwaste.entity.enums.RedemptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entitas PointRedemption — Transaksi Penukaran Poin Green Wallet.
 *
 * <p>Ketika warga ingin menukar poin di {@link GreenWallet} dengan reward/insentif
 * (seperti voucher, diskon tagihan, atau hadiah fisik), sebuah record
 * {@code PointRedemption} dibuat untuk mencatat dan melacak transaksi tersebut.</p>
 *
 * <p>Alur penukaran:</p>
 * <pre>
 *   Citizen request → PointRedemption [PENDING] → Admin review
 *       ├── Disetujui → [APPROVED] + GreenWallet.redeemPoints() dipanggil + rewardCode dikirim
 *       └── Ditolak  → [REJECTED] + GreenWallet.rollbackRedemption() dipanggil
 * </pre>
 *
 * <p><b>OOP:</b> Extends {@link BaseEntity} untuk mendapatkan id dan timestamps.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "point_redemptions",
       indexes = {
           @Index(name = "idx_redemption_citizen", columnList = "citizen_id"),
           @Index(name = "idx_redemption_status", columnList = "status")
       })
public class PointRedemption extends BaseEntity {

    public PointRedemption() {
        super();
    }

    /**
     * Warga pemilik poin yang melakukan penukaran.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    /**
     * Jumlah poin yang ditukarkan.
     */
    @Column(name = "points_redeemed", nullable = false)
    private double pointsRedeemed;

    /**
     * Status penukaran saat ini.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RedemptionStatus status = RedemptionStatus.PENDING;

    /**
     * Deskripsi reward yang diminta (misal: "Voucher Belanja Rp 50.000",
     * "Diskon Tagihan Listrik", atau "Donasi ke Bank Sampah").
     */
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * Kode unik reward yang dikirimkan kepada warga setelah penukaran disetujui.
     * Null jika masih PENDING atau REJECTED.
     */
    @Column(name = "reward_code", length = 100)
    private String rewardCode;

    /**
     * Catatan dari admin saat menyetujui atau menolak penukaran.
     */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /**
     * Referensi ID item reward dari katalog ({@link com.smartwaste.entity.RewardItem}).
     * Nullable — untuk mendukung permintaan penukaran bebas (tanpa katalog).
     */
    @Column(name = "reward_item_id", length = 36)
    private String rewardItemId;

    /**
     * Konstruktor untuk membuat permintaan penukaran baru.
     *
     * @param citizen        warga yang meminta penukaran
     * @param pointsRedeemed jumlah poin yang ingin ditukar
     * @param description    deskripsi reward yang diminta
     */
    public PointRedemption(Citizen citizen, double pointsRedeemed, String description) {
        this.citizen = citizen;
        this.pointsRedeemed = pointsRedeemed;
        this.description = description;
        this.status = RedemptionStatus.PENDING;
    }
    public Citizen getCitizen() { return citizen; }
    public void setCitizen(Citizen citizen) { this.citizen = citizen; }
    public double getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(double pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }
    public RedemptionStatus getStatus() { return status; }
    public void setStatus(RedemptionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRewardCode() { return rewardCode; }
    public void setRewardCode(String rewardCode) { this.rewardCode = rewardCode; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public String getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(String rewardItemId) { this.rewardItemId = rewardItemId; }
}
