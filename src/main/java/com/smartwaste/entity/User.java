package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Abstract class yang merepresentasikan pengguna dalam sistem Smart Community Waste.
 *
 * <p><b>OOP Concept — Abstract Class & Inheritance:</b></p>
 * <ul>
 *   <li>Kelas ini <b>tidak bisa di-instantiate</b> secara langsung (abstract).</li>
 *   <li>Method {@link #getRole()} bersifat abstract, memaksa setiap subclass
 *       ({@link Admin}, {@link Citizen}, {@link Collector}) mendefinisikan role-nya sendiri.</li>
 *   <li>Mewarisi field universal dari {@link BaseEntity} (id, timestamps).</li>
 * </ul>
 *
 * <p><b>OOP Concept — Polymorphism:</b>
 * Method {@code getRole()} bersifat abstract dan di-override oleh setiap subclass
 * dengan implementasi berbeda — inilah Method Overriding (Runtime Polymorphism).</p>
 *
 * <p><b>OOP Concept — Interface:</b>
 * Mengimplementasikan {@link UserDetails} dari Spring Security, memungkinkan semua
 * subclass digunakan langsung sebagai principal autentikasi JWT.</p>
 *
 * <p>Menggunakan strategi {@link InheritanceType#JOINED} sehingga tabel {@code users}
 * menyimpan field umum, dan setiap subclass punya tabelnya sendiri yang terhubung
 * via foreign key. Ini demonstrasi OOP yang paling "jujur" di level database.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class User extends BaseEntity implements UserDetails {

    /**
     * Nama lengkap pengguna.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Alamat email — digunakan sebagai username untuk login.
     * Unik di seluruh sistem (antar semua tipe user).
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Password yang sudah di-hash menggunakan BCrypt.
     * Field ini TIDAK pernah dikembalikan ke client (dikecualikan di DTO).
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Nomor telepon pengguna (opsional).
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Status aktif/nonaktif akun.
     * Admin dapat menonaktifkan akun tanpa menghapus datanya (soft disable).
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Status apakah MFA (Multi-Factor Authentication) diaktifkan.
     * Jika true, user wajib memasukkan kode OTP dari Google Authenticator setelah login.
     */
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    /**
     * Secret key yang digunakan untuk generate TOTP (Time-based One-Time Password).
     * Disimpan terenkripsi di database. Hanya ada jika user mengaktifkan MFA.
     */
    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    // ==================== Abstract Method (Polymorphism) ====================

    /**
     * Method abstract yang wajib di-override oleh setiap subclass.
     *
     * <p>Setiap subclass mengembalikan nilai berbeda:</p>
     * <ul>
     *   <li>{@link Admin#getRole()} → {@code "ADMIN"}</li>
     *   <li>{@link Citizen#getRole()} → {@code "CITIZEN"}</li>
     *   <li>{@link Collector#getRole()} → {@code "COLLECTOR"}</li>
     * </ul>
     *
     * <p>Ini adalah demonstrasi <b>Runtime Polymorphism</b> — satu variabel bertipe
     * {@code User} bisa memanggil {@code getRole()} dan mendapatkan hasil berbeda
     * tergantung dari subclass aktualnya.</p>
     *
     * @return string nama role user ("ADMIN", "CITIZEN", atau "COLLECTOR")
     */
    public abstract String getRole();

    // ==================== UserDetails Implementation (Interface) ====================

    /**
     * Mengembalikan daftar authorities (role) untuk Spring Security.
     * Format: "ROLE_ADMIN", "ROLE_CITIZEN", atau "ROLE_COLLECTOR".
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRole()));
    }

    /**
     * Mengembalikan email sebagai username (identifier unik untuk autentikasi).
     */
    @Override
    public String getUsername() {
        return this.email;
    }

    /**
     * Akun tidak pernah expired dalam sistem ini.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Akun terkunci jika {@link #active} bernilai false.
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.active;
    }

    /**
     * Kredensial tidak pernah expired.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * User aktif jika field {@link #active} bernilai true.
     */
    @Override
    public boolean isEnabled() {
        return this.active;
    }
    // ==================== Manual Getters/Setters ====================
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
}
