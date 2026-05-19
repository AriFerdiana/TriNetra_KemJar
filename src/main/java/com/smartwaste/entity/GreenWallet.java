package com.smartwaste.entity;

import com.smartwaste.exception.InsufficientPointsException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entitas Green Wallet — Dompet Digital Poin Hijau milik setiap warga.
 *
 * <p>GreenWallet adalah inti dari fitur Gamifikasi sistem ini. Setiap kali
 * warga berhasil menyetorkan sampah ({@link WasteDeposit} CONFIRMED), poin
 * secara otomatis dikreditkan ke wallet ini via method {@link #addPoints(double)}.</p>
 *
 * <p><b>OOP Concept — Encapsulation:</b>
 * Method {@link #addPoints(double)} dan {@link #redeemPoints(double)} mengenkapsulasi
 * logika bisnis perubahan saldo. Client tidak bisa langsung mengubah {@code totalPoints}
 * atau {@code redeemedPoints} — semua harus melalui method yang tervalidasi.</p>
 *
 * <p><b>OOP Concept — Composition:</b>
 * Mewarisi id/timestamps dari {@link BaseEntity} dan memiliki relasi OneToOne dengan
 * {@link Citizen}.</p>
 *
 * <p>Catatan: {@code totalPoints} adalah akumulasi poin yang PERNAH diperoleh,
 * sedangkan {@code availablePoints} = totalPoints - redeemedPoints.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "green_wallets")
public class GreenWallet extends BaseEntity {

    public GreenWallet() {
        super();
    }

    /**
     * Warga pemilik wallet ini.
     * Relasi OneToOne — satu Citizen memiliki tepat satu GreenWallet.
     * Foreign key {@code citizen_id} ada di tabel {@code green_wallets}.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false, unique = true)
    private Citizen citizen;

    /**
     * Akumulasi total poin yang pernah diperoleh dari setoran sampah.
     * Nilai ini TIDAK pernah berkurang — hanya bertambah.
     */
    @Column(name = "total_points", nullable = false)
    private double totalPoints = 0.0;

    /**
     * Total poin yang sudah ditukar (redeemed).
     * Nilai ini bertambah setiap ada penukaran yang disetujui.
     */
    @Column(name = "redeemed_points", nullable = false)
    private double redeemedPoints = 0.0;

    @Column(name = "target_points")
    private Double targetPoints = 1000.0;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Konstruktor untuk membuat GreenWallet baru saat citizen pertama kali dibuat.
     *
     * @param citizen pemilik wallet
     */
    public GreenWallet(Citizen citizen) {
        this.citizen = citizen;
        this.totalPoints = 0.0;
        this.redeemedPoints = 0.0;
        this.targetPoints = 1000.0;
    }

    // ==================== Business Logic Methods (Encapsulation) ====================

    /**
     * Menghitung saldo poin yang tersedia untuk ditukar.
     *
     * <p>Formula: availablePoints = totalPoints - redeemedPoints</p>
     *
     * @return saldo poin yang bisa digunakan untuk redeem
     */
    public double getAvailablePoints() {
        return this.totalPoints - this.redeemedPoints;
    }

    public Double getTargetPoints() {
        return targetPoints;
    }

    public void setTargetPoints(Double targetPoints) {
        this.targetPoints = targetPoints;
    }

    /**
     * Mengkreditkan poin ke wallet setelah setoran sampah dikonfirmasi.
     *
     * <p><b>Encapsulation:</b> Logika penambahan poin dienkapsulasi di sini,
     * bukan dilakukan secara langsung oleh service (set field).</p>
     *
     * @param points jumlah poin yang akan ditambahkan (harus positif)
     * @throws IllegalArgumentException jika points tidak positif
     */
    public void addPoints(double points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Jumlah poin yang ditambahkan harus lebih dari 0.");
        }
        this.totalPoints += points;
    }

    /**
     * Mendebit poin dari wallet untuk penukaran reward.
     *
     * <p><b>Encapsulation:</b> Validasi saldo dilakukan di dalam method ini.
     * Jika saldo tidak cukup, {@link InsufficientPointsException} dilempar
     * dan ditangani oleh {@code GlobalExceptionHandler}.</p>
     *
     * @param points jumlah poin yang akan ditukar (harus positif dan tidak melebihi saldo)
     * @throws InsufficientPointsException jika saldo tidak mencukupi
     * @throws IllegalArgumentException    jika points tidak positif
     */
    public void redeemPoints(double points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Jumlah poin untuk penukaran harus lebih dari 0.");
        }
        if (points > getAvailablePoints()) {
            throw new InsufficientPointsException(points, getAvailablePoints());
        }
        this.redeemedPoints += points;
    }

    /**
     * Membatalkan penukaran poin (rollback) jika penukaran ditolak admin.
     *
     * @param points jumlah poin yang dikembalikan dari penukaran yang dibatalkan
     */
    public void rollbackRedemption(double points) {
        if (points > 0 && this.redeemedPoints >= points) {
            this.redeemedPoints -= points;
        }
    }

    public Citizen getCitizen() { return citizen; }
    public void setCitizen(Citizen citizen) { this.citizen = citizen; }
    public double getTotalPoints() { return totalPoints; }
    public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
    public double getRedeemedPoints() { return redeemedPoints; }
    public void setRedeemedPoints(double redeemedPoints) { this.redeemedPoints = redeemedPoints; }
}
