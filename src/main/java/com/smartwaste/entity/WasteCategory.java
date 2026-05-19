package com.smartwaste.entity;

import com.smartwaste.entity.enums.WasteType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entitas Kategori Sampah — mendefinisikan jenis-jenis sampah yang diterima sistem.
 *
 * <p>Setiap kategori memiliki {@link WasteType} yang menentukan jenis pemrosesannya,
 * dan {@code pointsPerKg} yang menjadi dasar kalkulasi poin di {@code PointCalculatorStrategy}.</p>
 *
 * <p><b>OOP Concept — Composition:</b>
 * Mewarisi id dan timestamps dari {@link BaseEntity}. Terhubung ke {@link WasteDeposit}
 * via relasi OneToMany.</p>
 *
 * <p><b>Hubungan dengan Polymorphism (Strategy Pattern):</b>
 * {@code type} (WasteType) digunakan oleh {@code PointCalculatorContext} untuk memilih
 * implementasi {@code PointCalculatorStrategy} yang tepat saat menghitung poin.</p>
 *
 * <p>Contoh data kategori:</p>
 * <pre>
 *   Nama: "Sampah Sisa Makanan"  | Type: ORGANIC   | pointsPerKg: 5.0
 *   Nama: "Botol Plastik PET"    | Type: INORGANIC  | pointsPerKg: 10.0
 *   Nama: "Baterai Bekas"        | Type: B3         | pointsPerKg: 25.0
 * </pre>
 */
@Getter
@Setter
@Entity
@Table(name = "waste_categories")
public class WasteCategory extends BaseEntity {

    public WasteCategory() {
        super();
        this.deposits = new ArrayList<>();
    }

    /**
     * Nama kategori sampah yang deskriptif.
     * Contoh: "Sampah Sisa Makanan", "Botol Plastik PET", "Baterai Li-ion".
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Deskripsi detail kategori sampah (contoh, cara pemilahan, bahaya potensial).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Tipe/jenis sampah — ORGANIC, INORGANIC, atau B3.
     * Disimpan sebagai string (bukan integer) agar mudah dibaca di database.
     * Digunakan oleh Strategy Pattern untuk memilih kalkulator poin yang tepat.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WasteType type;

    /**
     * Poin dasar yang diberikan per kilogram sampah kategori ini.
     * Nilai aktual akan dikalikan dengan multiplier di PointCalculatorStrategy.
     *
     * <p>Contoh: {@code pointsPerKg = 10.0} dan multiplier INORGANIC = 1.5
     * → poin akhir = 15.0 per kg.</p>
     */
    @Column(name = "points_per_kg", nullable = false)
    private double pointsPerKg;

    /**
     * URL ikon/gambar untuk kategori ini (ditampilkan di UI Thymeleaf).
     * Bisa berupa path lokal atau URL CDN.
     */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * Status aktif kategori. Admin bisa menonaktifkan kategori tertentu
     * tanpa menghapus datanya dari database.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // ==================== Relasi ====================

    /**
     * Daftar semua setoran sampah yang menggunakan kategori ini.
     *
     * <p>OneToMany — satu kategori bisa digunakan oleh banyak setoran.
     * {@code FetchType.LAZY} untuk performa optimal.</p>
     */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<WasteDeposit> deposits = new ArrayList<>();

    /**
     * Konstruktor untuk membuat WasteCategory baru.
     *
     * @param name        nama kategori
     * @param description deskripsi
     * @param type        tipe sampah (ORGANIC/INORGANIC/B3)
     * @param pointsPerKg poin dasar per kilogram
     * @param iconUrl     URL ikon
     */
    public WasteCategory(String name, String description,
                         WasteType type, double pointsPerKg, String iconUrl) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.pointsPerKg = pointsPerKg;
        this.iconUrl = iconUrl;
    }
    // ==================== Manual Getters/Setters ====================
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public WasteType getType() { return type; }
    public void setType(WasteType type) { this.type = type; }
    public double getPointsPerKg() { return pointsPerKg; }
    public void setPointsPerKg(double pointsPerKg) { this.pointsPerKg = pointsPerKg; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<WasteDeposit> getDeposits() { return deposits; }
    public void setDeposits(List<WasteDeposit> deposits) { this.deposits = deposits; }
}
