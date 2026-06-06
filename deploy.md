# 🚀 Panduan Lengkap Deployment Projek Java Spring Boot ke STB (Debian)

Panduan ini disesuaikan khusus untuk project **SmartWaste System** yang menggunakan **Maven** dan **MySQL/MariaDB**.

---

## 📌 Informasi Kredensial STB
- **Username:** `ictlab2024`
- **Password:** `ictlab2024`

> [!IMPORTANT]
> **Link Distribusi Domain Cloudflare:**  
> [Google Spreadsheet Domain Cloudflare](https://docs.google.com/spreadsheets/d/1cqgnX7yjxAeam2t_ZSEa49fac3fky4wdS_YazAdP5-8/edit?usp=sharing)

---

## 1️⃣ Koneksi Internet Menggunakan Kabel LAN
*(Pastikan STB dalam keadaan mati sebelum memulai)*

1. Pastikan laptop Anda sudah terhubung ke internet (via Wi-Fi kampus atau tethering HP).
2. Siapkan **Kabel LAN**.
3. Colokkan satu ujung kabel LAN ke port LAN di laptop Anda *(gunakan USB to LAN adapter jika perlu)*.
4. Colokkan ujung kabel LAN satunya lagi ke port LAN di **STB Debian** Anda.
5. Nyalakan STB.

---

## 2️⃣ Setting Bridge Connection di Laptop

1. Di laptop, tekan tombol `Windows + R`.
2. Ketik `ncpa.cpl` lalu tekan **Enter**.
3. Di jendela *Network Connections*, perhatikan dua koneksi utama:
   - **Wi-Fi:** (Sumber internet yang aktif).
   - **Ethernet / LAN:** (Koneksi ke STB, biasanya bertuliskan *Unidentified network*).
4. Klik kanan pada ikon **Wi-Fi**, lalu pilih **Properties**.
5. Masuk ke tab **Sharing** (paling atas sebelah kanan).
6. Centang kotak: `"Allow other network users to connect through this computer's Internet connection"`.
7. Pada dropdown *Home networking connection*, pilih nama **Adapter Ethernet/LAN** yang mengarah ke STB tadi. (Jika tidak ada dropdown, abaikan saja).
8. Klik **OK**.

---

## 3️⃣ Cek IP Koneksi STB 

1. Tekan tombol `Windows + R`, ketik `cmd`, lalu tekan **Enter**.
2. Ketik perintah berikut lalu tekan **Enter**:
   ```cmd
   arp -a
   ```
3. Cari bagian yang diawali dengan **Interface: 192.168.137.1** *(IP default Windows saat Sharing LAN)*.
4. Di bawahnya, cari IP dengan tipe **dynamic** (dinamis), misalnya `192.168.137.150` atau `192.168.137.2`.
   - *Abaikan IP yang berakhiran `.255` karena itu adalah IP Broadcast.*
   - Berdasarkan hasil `arp -a` Anda, IP STB Anda yang terdeteksi adalah **`192.168.137.40`** (tipe static, physical address `aa-13-02-77-0b-1e`).
5. Catat IP tersebut (**IP STB Anda: 192.168.137.40**).

---

## 4️⃣ Login ke STB Menggunakan SSH

> [!TIP]
> Tahap ini bisa dilakukan paralel antar kelompok jika menggunakan 1 STB untuk 2 kelompok. Berkoordinasilah dengan teman sekelompok Anda.

1. Buka Command Prompt (`cmd`).
2. Ketikkan perintah SSH berikut:
   ```cmd
   ssh ictlab2024@192.168.137.40
   ```
3. Saat diminta password, ketik `ictlab2024` lalu tekan **Enter**.
4. Jika berhasil masuk, tampilan terminal akan berubah menunjukkan Anda berada di dalam sistem Linux STB.

---

## 5️⃣ Test Koneksi Internet STB

Pastikan STB sudah mendapat akses internet dari laptop.
```bash
ping google.com
```
Jika muncul balasan (*Reply* / *Time* ms), maka koneksi berhasil. Tekan `Ctrl + C` untuk menghentikan ping. Anda siap lanjut ke tahap berikutnya.

---

## 6️⃣ Update Sistem & Instalasi Paket Prasyarat

Jalankan perintah ini satu per satu. Proses ini akan mendownload sekitar ±500MB, pastikan koneksi internet laptop/Wi-Fi Anda memadai.
```bash
sudo apt update
```
```bash
sudo apt install default-jre systemd-timesyncd ntpdate -y
```
*(Masukkan password `ictlab2024` jika diminta)*

---

## 7️⃣ Sinkronisasi Jam Sistem
> [!WARNING]
> Sangat penting! Jam yang tidak sinkron akan menyebabkan *Cloudflare Tunnel* gagal berjalan (error sertifikat SSL/HTTPS).

Jalankan perintah ini berurutan:
```bash
sudo ntpdate pool.ntp.org
sudo timedatectl set-ntp true
date
```
Pastikan *output* perintah `date` menunjukkan **waktu yang sama persis** dengan jam di laptop Anda.

---

## 8️⃣ Membuat Direktori Aplikasi di STB

Siapkan folder `/opt/praktikum` sebagai tempat menyimpan file `.jar` aplikasi Anda.
```bash
sudo mkdir -p /opt/praktikum
sudo chmod 777 /opt/praktikum
cd /opt
ls
```
Pastikan folder `praktikum` sudah terlihat.

---

## 9️⃣ Setup Database MariaDB (MySQL)

Project **SmartWaste System** menggunakan database MariaDB/MySQL sesuai dengan pengaturan file `application.yml`.

### A. Install Database
```bash
sudo apt install mariadb-server -y
```

### B. Konfigurasi Keamanan MariaDB
```bash
sudo mysql_secure_installation
```
Ikuti panduan pengisian prompt berikut:
- *Enter current password for root (enter for none):* **[Tekan Enter]**
- *Switch to unix_socket authentication [Y/n]:* **y**
- *Change the root password? [Y/n]:* **y**
- *New password:* **root**  *(atau sesuaikan, tapi ingat password ini!)*
- *Re-enter new password:* **root**
- *Remove anonymous users? [Y/n]:* **y**
- *Disallow root login remotely? [Y/n]:* **y**
- *Remove test database and access to it? [Y/n]:* **y**
- *Reload privilege tables now? [Y/n]:* **y**

### C. Membuat Database Aplikasi
Masuk ke console MariaDB:
```bash
sudo mysql -u root -p
```
*(Masukkan password yang dibuat di langkah sebelumnya)*

Eksekusi perintah SQL berikut untuk membuat database `db_tubes_pbo_trinetra`:
```sql
CREATE DATABASE db_tubes_pbo_trinetra;
ALTER USER 'root'@'localhost' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON db_tubes_pbo_trinetra.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

> [!NOTE]
> Sesuaikan username dan password di konfigurasi MariaDB ini agar sama dengan koneksi di `application.yml` (`spring.datasource.username` dan `spring.datasource.password`).
> **PASTIKAN JUGA** properti `server.port` di `application.yml` Anda telah diubah menjadi `8081` (Sesuai alokasi port kelompok Anda).

---

## 🔟 Build Project Spring Boot (Maven)

Lakukan tahap ini **di Laptop Anda**, bukan di STB.

1. Buka terminal/CMD di direktori project `SmartWaste System`.
2. Lakukan proses *build* menggunakan **Maven**:
   - **Windows / Mac / Linux:** 
     ```cmd
     mvn clean package
     ```
3. Buka folder `target` di dalam project Anda.
4. Temukan file jar hasil build (contoh: `smart-waste-system-0.0.1-SNAPSHOT.jar`).
   - *Abaikan file yang berakhiran `-plain.jar` jika ada.*
5. **Rename** file tersebut agar lebih mudah (misal: `app-smartwaste.jar`). Jika dalam satu STB ada dua kelompok, bedakan penamaannya (contoh: `app-smartwaste.jar` dan `app-kelompok2.jar`).

---

## 1️⃣1️⃣ Transfer File JAR ke STB (Menggunakan WinSCP)

1. Download dan buka aplikasi [WinSCP](https://winscp.net/eng/download.php).
2. Buat koneksi baru (*New Site*) dengan konfigurasi:
   - **File protocol:** SFTP *(Coba ubah ke SCP jika SFTP gagal)*
   - **Host name:** `192.168.137.40`
   - **Port number:** `22`
   - **User name:** `ictlab2024`
   - **Password:** `ictlab2024`
3. Klik **Login** *(Jika ada peringatan keamanan/sertifikat, klik Yes/Accept)*.
4. Layar akan terbelah dua:
   - **Kiri:** File di laptop Anda.
   - **Kanan:** File di STB.
5. Di panel kanan (STB), masuk ke folder: `/opt/praktikum`.
6. Di panel kiri (Laptop), cari file `app-smartwaste.jar`.
7. **Drag & Drop** file `app-smartwaste.jar` ke panel kanan. Tunggu hingga upload 100%.

> [!WARNING]
> Jika **SFTP/SCP** tetap gagal terhubung (meskipun STB bisa di-ping), lihat bagian **Lampiran** di akhir panduan ini untuk konfigurasi tambahan.

---

## 1️⃣2️⃣ Konfigurasi Systemd Service (Auto-Start)

Agar aplikasi otomatis berjalan ketika STB dinyalakan, kita akan membuat service systemd.

1. Jalankan perintah ini di SSH STB:
   ```bash
   sudo nano /etc/systemd/system/smartwaste.service
   ```
2. Paste konfigurasi berikut ke dalam editor nano:
   ```ini
   [Unit]
   Description=Aplikasi Spring Boot SmartWaste System (Port 8081)
   After=network.target

   [Service]
   User=root
   ExecStart=/usr/bin/java -jar /opt/praktikum/app-kelompok2.jar
   SuccessExitStatus=143
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

3. Simpan dan keluar dari Nano:
   - Tekan `Ctrl + O` lalu tekan **Enter** (untuk save).
   - Tekan `Ctrl + X` (untuk exit).

---

## 1️⃣3️⃣ Menjalankan Aplikasi Spring Boot

Jalankan serangkaian perintah berikut secara berurutan untuk meregistrasi dan menyalakan aplikasi:
```bash
sudo systemctl daemon-reload
sudo systemctl enable smartwaste
sudo systemctl start smartwaste
```

**Cek Status Aplikasi:**
```bash
sudo systemctl status smartwaste
```
*(Pastikan tidak ada pesan error dan berstatus **active (running)**)*

**Cek Log Aplikasi (Jika terjadi error):**
```bash
sudo journalctl -u smartwaste.service -n 50 --no-pager
```

**Testing Aplikasi:**
Buka browser di laptop Anda dan akses:
`http://192.168.137.40:8081`

Lakukan pengujian fungsionalitas minimun untuk memastikan fitur dan database sudah terhubung dengan baik!

---

## 1️⃣4️⃣ Setup CloudFlare Tunnel (Akses Publik)

Agar aplikasi dapat diakses dari luar jaringan (Internet Publik), gunakan Cloudflare Tunnel.

1. **Install Cloudflared di STB (Copy paste secara berurutan):**
   ```bash
   # Add cloudflare gpg key
   sudo mkdir -p --mode=0755 /usr/share/keyrings
   curl -fsSL https://pkg.cloudflare.com/cloudflare-public-v2.gpg | sudo tee /usr/share/keyrings/cloudflare-public-v2.gpg >/dev/null
   
   # Add repo ke apt
   echo 'deb [signed-by=/usr/share/keyrings/cloudflare-public-v2.gpg] https://pkg.cloudflare.com/cloudflared any main' | sudo tee /etc/apt/sources.list.d/cloudflared.list
   
   # Update & Install
   sudo apt-get update && sudo apt-get install cloudflared
   ```

2. Karena Anda sudah mendapatkan token dan domain, jalankan perintah instalasi tunnel berikut di STB:
   ```bash
   sudo cloudflared service install eyJhIjoiNWE5NzY0ZGZmNzM5YzMwNTIzZjgwNDZiMzg5YWM2M2MiLCJ0IjoiYzA3MmNhMDUtNzY2MS00NDI4LTk1MWItMjdhNDAwMmM1ZDQ0IiwicyI6IlpHSTJPVFl6TmpndE9UTm1aQzAwT0RGaUxXSm1aalV0TkRFME1UZzJNVEZsTURJeCJ9
   ```
3. Setelah proses selesai, akses aplikasi Anda melalui domain yang telah disediakan:
   **➡️ [https://pbo-6b.node-f3a17c.my.id](https://pbo-6b.node-f3a17c.my.id)**
   - *Jika muncul pesan `Bad Gateway`, berarti `server.port` di file `application.yml` Anda belum diset ke `8081`.*

*(Catatan: Jika ada kendala teknis terkait Cloudflare, segera hubungi aslab terkait).*

---

## 🛠 Lampiran: Solusi Gagal WinSCP (SFTP)

Jika Anda tidak bisa mengakses STB menggunakan protokol SFTP di WinSCP, instal paket server SFTP di STB.

1. Di terminal SSH STB, jalankan:
   ```bash
   sudo apt install openssh-sftp-server -y
   ```
   > [!CAUTION]
   > Jika muncul prompt konfigurasi selama instalasi, **WAJIB** memilih `keep the local version currently installed`. Memilih opsi yang salah akan membuat SSH STB tidak bisa diakses!

2. Restart service SSH:
   ```bash
   sudo systemctl restart sshd
   ```
3. Coba koneksikan ulang aplikasi WinSCP atau Putty Anda.
