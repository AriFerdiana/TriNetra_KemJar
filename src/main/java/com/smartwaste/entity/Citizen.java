package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entitas Citizen (Warga) — subclass dari {@link User} untuk peran warga komunitas.
 *
 * <p><b>OOP Concept — Inheritance & Polymorphism:</b>
 * Mewarisi semua field dari {@link User} dan {@link BaseEntity}, lalu meng-override
 * {@link User#getRole()} untuk mengembalikan {@code "CITIZEN"}.</p>
 *
 * <p><b>Relasi Penting:</b></p>
 * <ul>
 *   <li><b>OneToOne</b> dengan {@link GreenWallet} — setiap warga punya 1 dompet poin digital.</li>
 *   <li><b>OneToMany</b> dengan {@link WasteDeposit} — seorang warga bisa punya banyak setoran.</li>
 *   <li><b>OneToMany</b> dengan {@link PointRedemption} — riwayat penukaran poin.</li>
 *   <li><b>OneToMany</b> dengan {@link ChatLog} — riwayat chat dengan AI Mistral.</li>
 * </ul>
 *
 * <p>JPA akan membuat tabel {@code citizens} yang terhubung ke {@code users} via JOIN.</p>
 *
 * <p><b>Hak Akses Citizen (RBAC):</b></p>
 * <ul>
 *   <li>Menyetorkan sampah</li>
 *   <li>Melihat riwayat setoran dan saldo poin</li>
 *   <li>Menukar poin (redeem)</li>
 *   <li>Berinteraksi dengan chatbot Mistral AI</li>
 * </ul>
 */
@Getter
@Setter
@Entity
@Table(name = "citizens")
@DiscriminatorValue("CITIZEN")
@PrimaryKeyJoinColumn(name = "user_id")
public class Citizen extends User {

    /**
     * Nomor Induk Kependudukan warga (NIK KTP).
     * Unik untuk setiap warga yang terdaftar.
     */
    @Column(name = "nik", unique = true, length = 16)
    private String nik;

    /**
     * Alamat lengkap tempat tinggal warga.
     */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /**
     * RT/RW tempat tinggal warga (untuk pengelompokan wilayah).
     */
    @Column(name = "rt_rw", length = 10)
    private String rtRw;

    /**
     * Nama kelurahan tempat tinggal warga.
     */
    @Column(name = "kelurahan", length = 100)
    private String kelurahan;

    // ==================== Relasi (OOP Composition) ====================

    /**
     * Dompet digital poin hijau milik warga (Green Wallet).
     *
     * <p>Relasi: OneToOne — satu warga memiliki tepat satu GreenWallet.
     * {@code cascade = ALL} memastikan wallet ikut dibuat saat citizen dibuat,
     * dan ikut dihapus saat citizen dihapus.</p>
     */
    @OneToOne(mappedBy = "citizen", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
              orphanRemoval = true)
    private GreenWallet wallet;

    /**
     * Daftar setoran sampah yang pernah dilakukan oleh warga ini.
     *
     * <p>Relasi: OneToMany — satu citizen bisa memiliki banyak WasteDeposit.
     * Menggunakan {@code FetchType.LAZY} untuk performa (tidak di-load sebelum diperlukan).</p>
     */
    @OneToMany(mappedBy = "citizen", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<WasteDeposit> deposits = new ArrayList<>();

    /**
     * Riwayat penukaran poin yang pernah dilakukan.
     */
    @OneToMany(mappedBy = "citizen", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<PointRedemption> redemptions = new ArrayList<>();

    /**
     * Riwayat percakapan dengan AI Chatbot Mistral.
     */
    @OneToMany(mappedBy = "citizen", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ChatLog> chatLogs = new ArrayList<>();

    /**
     * Konstruktor untuk membuat Citizen baru dari DTO registrasi.
     *
     * @param name     nama lengkap warga
     * @param email    alamat email
     * @param password password yang sudah di-hash BCrypt
     * @param phone    nomor telepon
     * @param nik      NIK KTP
     * @param address  alamat lengkap
     */
    public Citizen(String name, String email, String password,
                   String phone, String nik, String address) {
        this.setName(name);
        this.setEmail(email);
        this.setPassword(password);
        this.setPhone(phone);
        this.nik = nik;
        this.address = address;
    }

    /**
     * Override method abstract dari {@link User}.
     *
     * @return string "CITIZEN"
     */
    @Override
    public String getRole() {
        return "CITIZEN";
    }

    // ==================== Helper Methods ====================

    /**
     * Helper untuk menambahkan deposit ke list dan set relasi balik.
     *
     * @param deposit transaksi setoran yang akan ditambahkan
     */
    public void addDeposit(WasteDeposit deposit) {
        this.deposits.add(deposit);
        deposit.setCitizen(this);
    }
    /**
     * Manual No-args constructor to ensure compilation.
     */
    public Citizen() {
        super();
        this.deposits = new ArrayList<>();
        this.redemptions = new ArrayList<>();
        this.chatLogs = new ArrayList<>();
    }

    // ==================== Manual Getters/Setters ====================
    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRtRw() { return rtRw; }
    public void setRtRw(String rtRw) { this.rtRw = rtRw; }
    public String getKelurahan() { return kelurahan; }
    public void setKelurahan(String kelurahan) { this.kelurahan = kelurahan; }
    public GreenWallet getWallet() { return wallet; }
    public void setWallet(GreenWallet wallet) { this.wallet = wallet; }
    public List<WasteDeposit> getDeposits() { return deposits; }
    public void setDeposits(List<WasteDeposit> deposits) { this.deposits = deposits; }
    public List<PointRedemption> getRedemptions() { return redemptions; }
    public void setRedemptions(List<PointRedemption> redemptions) { this.redemptions = redemptions; }
    public List<ChatLog> getChatLogs() { return chatLogs; }
    public void setChatLogs(List<ChatLog> chatLogs) { this.chatLogs = chatLogs; }
}
