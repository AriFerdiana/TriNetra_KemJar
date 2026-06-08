# 🚀 Panduan Deployment NetraSphere di Ubuntu Server

Dokumen ini disusun khusus untuk tim infrastruktur (Server Administrator) yang bertugas men-*deploy* aplikasi NetraSphere ke server Ubuntu.

Aplikasi Web (*Application Layer*) **sudah 100% diamankan** dengan standar DevSecOps tertinggi (MFA, JWT, AES, Brute Force Protection, XSS/SQLi Protections, dll). Tugas Anda di server Ubuntu adalah mengaktifkan lapisan infrastruktur keamanan (*Network & Host Security*).

---

## TAHAP 1: Persiapan Aplikasi (Zero Trust Architecture)

Aplikasi ini menggunakan konsep **Zero Trust** via Docker. Database dan Backend tidak boleh diekspos ke publik, dan hanya bisa diakses melalui Nginx Reverse Proxy.

1. **Tarik (Pull) Kode Terbaru**
   ```bash
   git pull origin main
   ```
2. **Siapkan Environment Variables**
   ```bash
   cp .env.example .env
   nano .env
   ```
   *(Isi `MYSQL_ROOT_PASSWORD`, rahasia JWT/AES, dan masukkan `MISTRAL_API_KEY` asli untuk mengaktifkan AI Threat Analysis)*.

3. **Jalankan Aplikasi via Docker Compose**
   ```bash
   docker-compose up -d --build
   ```
   *Catatan:* Langkah ini otomatis mengaktifkan **Nginx Reverse Proxy Security** dan **WAF Sederhana** (menutupi versi server, memblokir method HTTP ilegal, dan menyisipkan Security Headers) yang sudah dikonfigurasi di folder `nginx/conf.d`.

---

## TAHAP 2: Host Security & Firewall (IDS/IPS Sederhana)

Kita sudah menyediakan script otomatis untuk mengamankan Ubuntu dari ancaman luar menggunakan UFW (Uncomplicated Firewall).

1. **Jalankan Script Hardening Firewall**
   ```bash
   sudo chmod +x setup-ubuntu-firewall.sh
   sudo ./setup-ubuntu-firewall.sh
   ```
   *Script ini akan memblokir semua port, membatasi percobaan login SSH (mencegah brute force server), dan hanya membuka port 80/443 (HTTP/HTTPS).*

2. **Install Fail2Ban (Opsional - Pelengkap IPS)**
   ```bash
   sudo apt update && sudo apt install fail2ban -y
   sudo systemctl enable fail2ban --now
   ```

---

## TAHAP 3: Logging & Monitoring (Suricata + Wazuh + Telegram)

Aplikasi NetraSphere telah diprogram untuk otomatis menulis log ancaman keamanan (*brute force*, *path traversal*, *malicious file upload*) ke dalam file `logs/smartwaste-security.log` di dalam server.

Tugas Anda adalah menghubungkan log ini ke Wazuh dan Suricata.

1. **Konfigurasi Wazuh Agent di Ubuntu**
   Buka file konfigurasi Wazuh Agent (`/var/ossec/etc/ossec.conf`) dan tambahkan blok berikut agar Wazuh membaca log aplikasi web:
   ```xml
   <localfile>
     <log_format>syslog</log_format>
     <location>/path/ke/folder/proyek/logs/smartwaste-security.log</location>
   </localfile>
   ```
   *(Ganti `/path/ke/folder/proyek` dengan path absolut direktori NetraSphere Anda)*.
   Restart Wazuh Agent: `sudo systemctl restart wazuh-agent`.

2. **Konfigurasi Suricata (Network IDS)**
   Pastikan Suricata berjalan memantau interface internet utama (misal `eth0` atau `ens33`) untuk mendeteksi *DDoS* atau serangan *port scanning* yang mengarah ke IP publik Ubuntu.

3. **Notifikasi Telegram di Wazuh Manager**
   Di server **Wazuh Manager** Anda, buat *custom integration* script (menggunakan Python atau Bash) yang menangkap *Alert Level 7+* lalu mengirimkannya ke Telegram API via *webhook*.
   *Referensi:* Anda dapat menggunakan script `custom-telegram` milik Wazuh yang mengirim HTTP POST ke `https://api.telegram.org/bot<BOT_TOKEN>/sendMessage`.

---

## Checklist Final Kelulusan Tugas:

| Bagian | Tanggung Jawab | Status |
| :--- | :--- | :---: |
| Password Hashing | Developer Web | ✅ Selesai |
| Proteksi SQLi & XSS | Developer Web | ✅ Selesai |
| Rate Limiting / Brute Force | Developer Web | ✅ Selesai |
| Secure File Upload | Developer Web | ✅ Selesai |
| Enkripsi AES & JWT & MFA | Developer Web | ✅ Selesai |
| CI/CD & Container Security | Developer Web | ✅ Selesai |
| Dashboard AI Threat Analyst | Developer Web | ✅ Selesai |
| **Reverse Proxy & WAF** | **Server Admin (Docker)** | ✅ Otomatis Jalan |
| **Firewall & IPS** | **Server Admin (Ubuntu)** | 🔲 Jalankan Tahap 2 |
| **Suricata + Wazuh (Telegram)** | **Server Admin (Ubuntu)** | 🔲 Jalankan Tahap 3 |

**Selamat Bertugas! Pertahankan server tetap aman dari serangan Red Team! 🛡️**
