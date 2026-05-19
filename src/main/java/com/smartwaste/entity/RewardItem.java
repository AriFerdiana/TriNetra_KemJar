package com.smartwaste.entity;

import jakarta.persistence.*;

/**
 * Entitas Katalog Hadiah (Reward Item) — Item yang bisa ditukar warga dengan poin.
 *
 * <p><b>OOP Concept — Inheritance:</b> Extends {@link BaseEntity} untuk id dan timestamps.</p>
 *
 * <p>Admin mendefinisikan katalog hadiah di sini, warga memilih saat melakukan
 * penukaran poin. Stok dikurangi otomatis saat redemption di-approve.</p>
 */
@Entity
@Table(name = "reward_items")
public class RewardItem extends BaseEntity {

    public RewardItem() {
        super();
    }

    /** Nama hadiah, contoh: "Voucher Belanja Rp 50.000" */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Deskripsi singkat hadiah */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Emoji/ikon untuk representasi visual hadiah */
    @Column(name = "icon", length = 20)
    private String icon = "🎁";

    /** Jumlah poin yang harus dibayar warga untuk mendapatkan hadiah ini */
    @Column(name = "points_cost", nullable = false)
    private double pointsCost;

    /** Stok tersedia. -1 = unlimited */
    @Column(name = "stock", nullable = false)
    private int stock = -1;

    /** Level minimum warga untuk menukar hadiah ini, contoh: "Gold Champion" */
    @Column(name = "required_level", length = 50)
    private String requiredLevel = "Green Starter";

    /** Flag untuk menampilkan label 'HOT' atau 'Populer' di katalog */
    @Column(name = "is_popular", nullable = false)
    private boolean isPopular = false;

    /** Status aktif — jika nonaktif, hadiah tidak ditampilkan ke warga */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Flag untuk menandakan apakah item ini adalah donasi */
    @Column(name = "is_donation", nullable = false)
    private boolean isDonation = false;

    public RewardItem(String name, String description, String icon, double pointsCost, int stock, String requiredLevel, boolean isPopular, boolean isDonation) {
        this.name = name;
        this.description = description;
        this.icon = icon != null && !icon.isBlank() ? icon : "🎁";
        this.pointsCost = pointsCost;
        this.stock = stock;
        this.requiredLevel = requiredLevel != null ? requiredLevel : "Green Starter";
        this.isPopular = isPopular;
        this.isDonation = isDonation;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public double getPointsCost() { return pointsCost; }
    public void setPointsCost(double pointsCost) { this.pointsCost = pointsCost; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(String requiredLevel) { this.requiredLevel = requiredLevel; }
    public boolean isPopular() { return isPopular; }
    public void setPopular(boolean popular) { this.isPopular = popular; }
    public boolean isDonation() { return isDonation; }
    public void setDonation(boolean donation) { isDonation = donation; }

    /** Cek apakah stok masih tersedia */
    public boolean isAvailable() {
        return active && (stock == -1 || stock > 0);
    }

    /** Kurangi stok 1 unit. Jika unlimited (-1), tidak ada perubahan. */
    public void decreaseStock() {
        if (stock > 0) stock--;
    }
}
