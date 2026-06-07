#!/bin/bash
# ========================================================
# Skrip Otomatisasi Ubuntu UFW Firewall (Zero Trust)
# Dibuat untuk: Project SmartWaste / TriNetra
# ========================================================

echo "Memulai konfigurasi UFW Firewall untuk Zero Trust..."

# 1. Pastikan UFW dinonaktifkan sementara untuk direset
sudo ufw disable

# 2. Reset ke Default yang Aman (Zero Trust Principle: Deny by Default)
echo "[1/4] Mengatur Default Deny..."
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 3. Buka Port yang Diizinkan (Whitelist)
echo "[2/4] Mengatur Whitelist Port..."
# Buka port SSH agar tidak kehilangan remote access
sudo ufw allow 22/tcp

# Buka port HTTP dan HTTPS HANYA untuk Nginx Reverse Proxy
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

echo "[3/4] Catatan: Port 8081 (Spring Boot), 3306 (MySQL), 6379 (Redis) SENGAJA TIDAK DIBUKA untuk mengisolasi backend."

# 4. Aktifkan Firewall secara paksa
echo "[4/4] Mengaktifkan UFW..."
sudo ufw --force enable

echo "========================================================"
echo "Konfigurasi Firewall Berhasil!"
echo "Server Ubuntu Anda sekarang menerapkan Zero Trust Network."
echo "Cek status dengan menjalankan: sudo ufw status numbered"
echo "========================================================"
