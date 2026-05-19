package com.smartwaste.dto.request;

import jakarta.validation.constraints.*;

/**
 * DTO untuk registrasi warga (Citizen) baru.
 */
public class RegisterCitizenRequest {

    @NotBlank(message = "Nama tidak boleh kosong")
    @Pattern(regexp = "^[a-zA-Z\\s]{3,100}$", message = "Nama hanya boleh mengandung huruf dan spasi (minimal 3 karakter)")
    private String name;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password minimal 8 karakter dengan kombinasi huruf besar, kecil, angka, dan karakter spesial (@$!%*?&)")
    private String password;

    @NotBlank(message = "Nomor telepon tidak boleh kosong")
    @Pattern(regexp = "^\\+62[0-9]{9,13}$", message = "Format nomor telepon tidak valid (harus diawali +62 dan diikuti 9-13 digit angka)")
    private String phone;

    @NotBlank(message = "NIK tidak boleh kosong")
    @Pattern(regexp = "^[0-9]{16}$", message = "NIK harus berupa 16 digit angka")
    private String nik;

    @NotBlank(message = "Alamat tidak boleh kosong")
    private String address;

    @NotBlank(message = "RT/RW tidak boleh kosong")
    @Pattern(regexp = "^[0-9]{3}/[0-9]{3}$", message = "Format RT/RW harus 000/000")
    private String rtRw;

    @NotBlank(message = "Kelurahan tidak boleh kosong")
    private String kelurahan;

    public RegisterCitizenRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRtRw() { return rtRw; }
    public void setRtRw(String rtRw) { this.rtRw = rtRw; }
    public String getKelurahan() { return kelurahan; }
    public void setKelurahan(String kelurahan) { this.kelurahan = kelurahan; }
}
