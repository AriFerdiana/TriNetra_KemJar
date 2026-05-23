/**
 * Citizen Dashboard Logic
 * Handles Alpine.js data, Leaflet Map, and Chart.js
 */

document.addEventListener('alpine:init', () => {
    Alpine.data('dashboardData', () => ({
        showLiveTracker: false,
        activeTab: 'dashboard',
        citizenSidebarOpen: window.innerWidth >= 1024,
        chatMessages: [],
        chatInput: '',
        chatLoading: false,
        selectedCategory: '',
        pointsPerKg: 0,
        weight: '',
        notes: '',
        redeemPoints: '',
        redeemDescription: '',
        selectedRewardId: '',
        selectedRewardIcon: '',
        redeemQuantity: 1,
        fulfillmentMethod: 'delivery',
        selectedRewardPoints: null,
        rewardList: window.citizenServerData.rewards,
        rewardFilter: 'all',
        
        // Chat State
        chatOpen: false,
        chatUsers: [],
        selectedChatUser: null,
        chatHistory: [],
        chatMessageInput: '',
        unreadChatCount: 0,
        profileMenuOpen: false,
        showEditProfileModal: false,
        showPasswordModal: false,
        showQRModal: false,
        citizenNIK: window.citizenServerData.nik || '0000000000000000',

        showMyQR() {
            this.showQRModal = true;
            this.$nextTick(() => {
                const qrContainer = document.getElementById('citizenQRCode');
                if (qrContainer) {
                    qrContainer.innerHTML = '';
                    new QRCode(qrContainer, {
                        text: this.citizenNIK,
                        width: 180,
                        height: 180,
                        colorDark: "#064e3b",
                        colorLight: "#ffffff",
                        correctLevel: QRCode.CorrectLevel.H
                    });
                }
            });
        },

        // Fleet/Armada State
        fleetStatus: 'idle', // 'idle', 'en-route', 'nearby', 'arrived'
        fleetProgress: 0,
        pickupEta: 15,
        isPickupRequested: false,
        fleetNotification: true,
        driverInfo: {
            name: 'Pak Budi Santoso',
            id: 'TRK-029',
            rating: 4.9,
            photo: '👤'
        },

        // Glossary State
        openGlossary: null,

        // News Modal State
        openNewsModal: false,
        selectedNews: null,
        openNewsDetail(title, content, type, date, author, img) {
            this.selectedNews = { title, content, type, date, author, img };
            this.openNewsModal = true;
        },

        // Quiz State
        quizCurrentQuestion: 0,
        quizScore: 0,
        quizFinished: false,
        quizQuestions: [
            { q: "Kulit pisang termasuk kategori sampah apa?", a: "Organik", options: ["Organik", "Anorganik", "B3"] },
            { q: "Baterai bekas sebaiknya dibuang ke mana?", a: "TPS B3 Khusus", options: ["Tempat Sampah Biasa", "TPS B3 Khusus", "Selokan"] },
            { q: "Botol plastik bekas minuman sebaiknya...", a: "Dicuci lalu dipilah", options: ["Langsung dibakar", "Dicuci lalu dipilah", "Dibuang ke sungai"] },
            { q: "Styrofoam termasuk sampah yang...", a: "Sulit didaur ulang", options: ["Mudah terurai", "Sulit didaur ulang", "Bisa dikompos"] },
            { q: "Apa manfaat utama mendaur ulang kertas?", a: "Menyelamatkan pohon", options: ["Menambah polusi", "Menyelamatkan pohon", "Menguras air"] },
            { q: "Sampah B3 ditandai dengan warna label...", a: "Merah", options: ["Merah", "Kuning", "Hijau"] },
            { q: "Botol PET biasanya memiliki angka kode daur ulang...", a: "1", options: ["1", "2", "5"] },
            { q: "Pupuk kompos dibuat dari pengolahan sampah...", a: "Organik", options: ["Plastik", "Organik", "Logam"] },
            { q: "Berapa kali kaca bisa didaur ulang?", a: "Selamanya", options: ["10 kali", "50 kali", "Selamanya"] },
            { q: "Prinsip 3R terdiri dari Reduce, Reuse, dan...", a: "Recycle", options: ["Renew", "Recycle", "Replace"] }
        ],
        
        answerQuiz(option) {
            if (option === this.quizQuestions[this.quizCurrentQuestion].a) {
                this.quizScore += 20;
            }
            if (this.quizCurrentQuestion < this.quizQuestions.length - 1) {
                this.quizCurrentQuestion++;
            } else {
                this.quizFinished = true;
                if(this.quizScore === 100) {
                    Swal.fire({
                        title: 'Luar Biasa! 🎉',
                        text: 'Skor Anda 100! Anda adalah Ahli Lingkungan sejati.',
                        icon: 'success',
                        confirmButtonColor: '#10b981'
                    });
                }
            }
        },
        
        resetQuiz() {
            this.quizCurrentQuestion = 0;
            this.quizScore = 0;
            this.quizFinished = false;
        },

        // Carbon Calculator State
        carbonPlastic: 0,
        carbonPaper: 0,
        carbonOrganic: 0,
        carbonMetal: 0,
        get carbonResult() {
            const co2 = (this.carbonPlastic * 2.5) + (this.carbonPaper * 0.9) + (this.carbonOrganic * 0.5) + (this.carbonMetal * 4.0);
            const water = (this.carbonPaper * 26.5) + (this.carbonOrganic * 2.0) + (this.carbonMetal * 14.0);
            const trees = (this.carbonPaper / 60);
            return { co2: co2.toFixed(1), water: Math.round(water), trees: trees.toFixed(2) };
        },

        // Eco-Calculator State
        calcCategory: 'organik',
        calcWeight: null,
        calculatePoints() {
            if (!this.calcWeight || this.calcWeight <= 0) return 0;
            let multiplier = 200;
            if (this.calcCategory === 'anorganik') multiplier = 500;
            else if (this.calcCategory === 'b3') multiplier = 1000;
            return Math.round(this.calcWeight * multiplier).toLocaleString();
        },
        calculateCarbon() {
            if (!this.calcWeight || this.calcWeight <= 0) return '0.00';
            let factor = 0.5;
            if (this.calcCategory === 'anorganik') factor = 2.5;
            else if (this.calcCategory === 'b3') factor = 4.0;
            return (this.calcWeight * factor).toFixed(2);
        },

        // House Map State
        selectedRoom: null,
        houseRooms: {
            kitchen: { label: 'Dapur 🍳', color: 'emerald', items: [
                { name: 'Sisa Sayuran & Buah', cat: 'Organik', icon: '🥦' },
                { name: 'Minyak Goreng Bekas', cat: 'B3 / Organik Cair', icon: '🛢️' },
                { name: 'Kemasan Plastik Bumbu', cat: 'Anorganik', icon: '🧴' },
                { name: 'Kardus Makanan Bersih', cat: 'Anorganik', icon: '📦' },
                { name: 'Ampas Kopi & Teh', cat: 'Organik', icon: '☕' },
            ]},
            bathroom: { label: 'Kamar Mandi 🚿', color: 'blue', items: [
                { name: 'Botol Sampo & Sabun', cat: 'Anorganik', icon: '🧴' },
                { name: 'Sikat Gigi Bekas', cat: 'Anorganik (Sulit)', icon: '🪥' },
                { name: 'Tisu & Pembalut', cat: 'Residu (Campuran)', icon: '🧻' },
                { name: 'Obat Kadaluarsa', cat: 'B3', icon: '💊' },
                { name: 'Botol Parfum Kaca', cat: 'Anorganik', icon: '🫙' },
            ]},
            living: { label: 'Ruang Tamu 🛋️', color: 'purple', items: [
                { name: 'Majalah & Koran', cat: 'Anorganik', icon: '📰' },
                { name: 'Baterai Remote', cat: 'B3', icon: '🔋' },
                { name: 'Lampu LED Mati', cat: 'B3', icon: '💡' },
                { name: 'Tas Plastik Belanja', cat: 'Anorganik (Kurangi)', icon: '🛍️' },
            ]},
            storage: { label: 'Gudang 📦', color: 'amber', items: [
                { name: 'Elektronik Rusak', cat: 'B3 (E-Waste)', icon: '💻' },
                { name: 'Cat & Kaleng Cat', cat: 'B3', icon: '🎨' },
                { name: 'Kardus Pindahan', cat: 'Anorganik', icon: '📦' },
                { name: 'Baju Bekas Layak', cat: 'Donasikan', icon: '👕' },
                { name: 'Aki/Baterai Mobil', cat: 'B3', icon: '🚗' },
            ]},
        },

        // FAQ State
        openFaq: null,
        faqData: [
            { q: 'Bolehkah membuang kotak pizza yang berminyak?', a: 'Tidak! Kotak pizza berminyak tidak bisa didaur ulang karena minyak merusak serat kertas. Masukkan ke sampah organik/residu, atau potong bagian bersihnya saja yang ke anorganik.' },
            { q: 'Apakah tisu basah termasuk sampah organik?', a: 'Tidak. Tisu basah mengandung plastik (polypropylene) yang tidak terurai. Ini termasuk sampah residu yang tidak bisa didaur ulang. Jangan gunakan berlebihan!' },
            { q: 'Bagaimana cara membuang minyak goreng bekas?', a: 'Jangan tuang ke wastafel atau selokan! Bekukan dalam wadah tertutup, lalu buang ke tempat pengumpulan minyak jelantah, atau hubungi petugas NetraSphere untuk penanganan khusus.' },
            { q: 'Apakah styrofoam bisa didaur ulang?', a: 'Secara teknis bisa, tapi sangat jarang karena biayanya mahal. Di Indonesia, styrofoam umumnya masuk sampah residu/B3. Sebaiknya hindari penggunaan styrofoam sama sekali.' },
            { q: 'Bolehkah buang baju bekas ke tempat sampah biasa?', a: 'Jangan! Baju bekas yang masih layak pakai sebaiknya didonasikan ke yayasan sosial atau bank pakaian. Jika sudah tidak layak, kumpulkan ke bank daur ulang tekstil khusus.' },
            { q: 'Apakah sampah elektronik (E-waste) berbahaya?', a: 'Ya, sangat berbahaya karena mengandung logam berat seperti merkuri dan timbal. Jangan dibuang bersama sampah rumah tangga!' },
            { q: 'Bagaimana cara mengolah sampah organik di rumah?', a: 'Gunakan komposter atau teknik biopori. Hasilnya bisa jadi pupuk tanaman yang sangat subur.' },
            { q: 'Apa bedanya plastik HDPE dan LDPE?', a: 'HDPE (Kode 2) biasanya kaku (botol sampo), sedangkan LDPE (Kode 4) biasanya lentur (kantong kresek/plastik wrap).' },
            { q: 'Apakah sachet kopi bisa didaur ulang?', a: 'Sangat sulit karena terdiri dari lapisan plastik dan metalized. Sebaiknya gunakan produk dengan kemasan besar atau didaur ulang jadi kerajinan (upcycling).' },
            { q: 'Mengapa harus memisahkan tutup botol?', a: 'Tutup botol seringkali terbuat dari jenis plastik yang berbeda (PP) dari botolnya (PET). Memisahkan keduanya membantu proses penggilingan di pabrik daur ulang.' },
        ],

        get filteredRewards() {
            let list = [...this.rewardList];
            if (this.rewardFilter === 'lowest') {
                return list.sort((a, b) => a.points - b.points);
            } else if (this.rewardFilter === 'highest') {
                return list.sort((a, b) => b.points - a.points);
            } else if (this.rewardFilter === 'popular') {
                return list.filter(r => r.isPopular).sort((a, b) => b.redeemCount - a.redeemCount);
            } else if (this.rewardFilter === 'redemptions') {
                return list.sort((a, b) => b.redeemCount - a.redeemCount);
            }
            return list;
        },
        
        notification: { 
            show: window.citizenServerData.notificationShow, 
            message: window.citizenServerData.notificationMessage, 
            type: window.citizenServerData.notificationType 
        },
        
        
        // Real-time Pickup Countdown Logic
        pickupTimer: {
            nextShift: 'Menunggu...',
            countdown: '00:00:00',
            isActive: false,
            start() {
                const update = () => {
                    const now = new Date();
                    const hour = now.getHours();
                    const morningStart = 7;
                    const morningEnd = 10;
                    const afternoonStart = 15;
                    const afternoonEnd = 18;

                    let target = new Date();
                    target.setSeconds(0);
                    target.setMilliseconds(0);

                    if (hour < morningStart) {
                        this.nextShift = 'SHIFT PAGI (07:00)';
                        target.setHours(morningStart, 0);
                        this.isActive = false;
                    } else if (hour >= morningStart && hour < morningEnd) {
                        this.nextShift = 'SHIFT PAGI AKTIF';
                        target.setHours(morningEnd, 0);
                        this.isActive = true;
                    } else if (hour < afternoonStart) {
                        this.nextShift = 'SHIFT SORE (15:00)';
                        target.setHours(afternoonStart, 0);
                        this.isActive = false;
                    } else if (hour >= afternoonStart && hour < afternoonEnd) {
                        this.nextShift = 'SHIFT SORE AKTIF';
                        target.setHours(afternoonEnd, 0);
                        this.isActive = true;
                    } else {
                        this.nextShift = 'SHIFT BESOK (07:00)';
                        target.setDate(target.getDate() + 1);
                        target.setHours(morningStart, 0);
                        this.isActive = false;
                    }

                    const diff = target - now;
                    if (diff <= 0) {
                        this.countdown = '00:00:00';
                        return;
                    }
                    const h = Math.floor(diff / 3600000);
                    const m = Math.floor((diff % 3600000) / 60000);
                    const s = Math.floor((diff % 60000) / 1000);
                    this.countdown = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
                };
                update();
                setInterval(update, 1000);
            }
        },

init() {
            this.pickupTimer.start();
            if(this.notification.show) {
                Swal.fire({
                    icon: this.notification.type === 'success' ? 'success' : 'error',
                    title: this.notification.type === 'success' ? 'Berhasil!' : 'Oops...',
                    text: this.notification.message,
                    confirmButtonColor: '#10b981',
                    borderRadius: '1.25rem'
                });
            }
            this.fetchUnreadChatCount();
            setInterval(() => this.fetchUnreadChatCount(), 10000);
            
            // Polling Notifications
            setInterval(() => this.pollNotifications(), 5000);
            
            // Start Fleet Simulation
            this.startFleetSimulation();
        },

        startFleetSimulation() {
            const now = new Date();
            const hour = now.getHours();
            // Simulation triggers if within shift hours (7-9 or 15-17)
            if ((hour >= 7 && hour < 9) || (hour >= 15 && hour < 17)) {
                this.fleetStatus = 'en-route';
                this.fleetProgress = 15;
                this.pickupEta = 12;
                
                setInterval(() => {
                    if (this.fleetProgress < 100) {
                        // Progress moves slowly
                        this.fleetProgress += 0.2;
                        if (this.fleetProgress > 75) {
                            this.fleetStatus = 'nearby';
                        }
                        // Update ETA based on progress
                        this.pickupEta = Math.max(1, Math.round(15 * (1 - this.fleetProgress/100)));
                    } else {
                        this.fleetStatus = 'arrived';
                        this.pickupEta = 0;
                    }
                }, 5000);
            }
        },

        requestPickup() {
            this.isPickupRequested = true;
            Swal.fire({
                icon: 'success',
                title: 'Penjemputan Dikonfirmasi!',
                text: 'Armada kami akan memprioritaskan rute ke lokasi Anda. Harap siapkan sampah Anda.',
                confirmButtonColor: '#10b981'
            });
        },

        toggleFleetNotification() {
            this.fleetNotification = !this.fleetNotification;
            const status = this.fleetNotification ? 'Aktif' : 'Nonaktif';
            Swal.fire({
                toast: true,
                position: 'top-end',
                icon: 'info',
                title: 'Notifikasi Armada ' + status,
                showConfirmButton: false,
                timer: 2000
            });
        },
        
        async pollNotifications() {
            try {
                const res = await fetch('/api/v1/notifications/unread');
                if (res.ok) {
                    const notifs = await res.json();
                    if (notifs && notifs.length > 0) {
                        for (let n of notifs) {
                            Swal.fire({
                                toast: true,
                                position: 'bottom-end',
                                icon: n.type === 'SUCCESS' ? 'success' : (n.type === 'ERROR' ? 'error' : 'info'),
                                title: n.title,
                                text: n.message,
                                showConfirmButton: false,
                                timer: 5000,
                                timerProgressBar: true
                            });
                            // Mark as read
                            await fetch('/api/v1/notifications/' + n.id + '/read', { method: 'POST' });
                        }
                    }
                }
            } catch(e) {}
        },

        scrollNews(direction) {
            const container = this.$refs.newsContainer;
            const scrollAmount = 400;
            if (direction === 'left') {
                container.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
            } else {
                container.scrollBy({ left: scrollAmount, behavior: 'smooth' });
            }
        },

        confirmRedeem() {
            if (!this.selectedRewardPoints || this.selectedRewardPoints <= 0) {
                Swal.fire({ icon: 'warning', title: 'Poin Tidak Valid', text: 'Silakan pilih hadiah dari katalog terlebih dahulu.', confirmButtonColor: '#f59e0b' });
                return;
            }
            if (!this.redeemQuantity || this.redeemQuantity < 1) {
                this.redeemQuantity = 1;
            }
            
            const itemPoints  = this.selectedRewardPoints * this.redeemQuantity;
            const ongkir      = this.fulfillmentMethod === 'delivery' ? 50 : 0;
            const totalPoints = itemPoints + ongkir;
            
            if (totalPoints > window.citizenServerData.availablePoints) {
                Swal.fire({
                    icon: 'error',
                    title: 'Saldo Tidak Cukup',
                    text: 'Poin Anda (' + window.citizenServerData.availablePoints.toLocaleString() + ' pts) tidak cukup untuk menukar ' + itemPoints.toLocaleString() + ' pts' + (ongkir > 0 ? ' + ongkir ' + ongkir + ' pts' : '') + '.',
                    confirmButtonColor: '#f59e0b'
                });
                return;
            }

            // Tier Check
            const selected = this.rewardList.find(r => r.name === this.redeemDescription);
            const levelOrder = {'Green Starter': 0, 'Silver Elite': 1, 'Gold Champion': 2};
            if (selected && levelOrder[window.citizenServerData.userLevel] < levelOrder[selected.requiredLevel]) {
                Swal.fire({
                    icon: 'lock',
                    title: 'Level Belum Cukup',
                    text: 'Hadiah ini membutuhkan level ' + selected.requiredLevel + '. Terus setor sampah untuk naik level!',
                    confirmButtonColor: '#f59e0b'
                });
                return;
            }

            const methodText    = this.fulfillmentMethod === 'delivery' ? 'Kirim ke Rumah 🚚' : 'Ambil di Eco-Center 🏪';
            const now           = new Date();
            const dateStr       = now.toLocaleDateString('id-ID', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
            const timeStr       = now.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' });
            const invoiceNo     = 'TKR-' + now.getFullYear() + String(now.getMonth()+1).padStart(2,'0') + String(now.getDate()).padStart(2,'0') + '-' + String(Math.floor(Math.random()*9000)+1000);

            const receiptHtml = `
                <div style="font-family:'Inter',sans-serif; text-align:left; margin: -8px -16px;">
                    <!-- Header Struk -->
                    <div style="background: linear-gradient(135deg,#f59e0b,#f97316); padding:20px 24px; text-align:center; border-radius:12px 12px 0 0;">
                        <div style="font-size:28px; margin-bottom:4px;">🌿</div>
                        <div style="color:white; font-size:18px; font-weight:900; letter-spacing:2px;">NETRA SPHERE</div>
                        <div style="color:rgba(255,255,255,0.8); font-size:10px; font-weight:700; letter-spacing:3px; text-transform:uppercase; margin-top:2px;">Struk Penukaran Poin</div>
                    </div>

                    <!-- Info Transaksi -->
                    <div style="background:#fffbeb; padding:12px 24px; border-bottom:1px dashed #fcd34d; display:flex; justify-content:space-between; align-items:center;">
                        <div>
                            <div style="font-size:9px; color:#92400e; font-weight:700; text-transform:uppercase; letter-spacing:1px;">No. Invoice</div>
                            <div style="font-size:11px; color:#1f2937; font-weight:800; font-family:monospace;">${invoiceNo}</div>
                        </div>
                        <div style="text-align:right;">
                            <div style="font-size:9px; color:#92400e; font-weight:700; text-transform:uppercase; letter-spacing:1px;">Tanggal</div>
                            <div style="font-size:10px; color:#1f2937; font-weight:700;">${dateStr}</div>
                            <div style="font-size:10px; color:#6b7280; font-weight:600;">${timeStr} WIB</div>
                        </div>
                    </div>

                    <!-- Atas garis potong -->
                    <div style="padding:0 24px; position:relative; height:16px; background:white;">
                        <div style="position:absolute; left:0; right:0; top:50%; border-top:2px dashed #e5e7eb;"></div>
                        <div style="position:absolute; left:-12px; top:0; width:24px; height:24px; background:#f3f4f6; border-radius:50%;"></div>
                        <div style="position:absolute; right:-12px; top:0; width:24px; height:24px; background:#f3f4f6; border-radius:50%;"></div>
                    </div>

                    <!-- Detail Item -->
                    <div style="padding:12px 24px 16px; background:white;">
                        <div style="font-size:9px; color:#6b7280; font-weight:700; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:10px;">Detail Item</div>
                        <div style="display:flex; align-items:center; gap:12px; background:#fafafa; border-radius:12px; padding:12px;">
                            <div style="font-size:28px; width:48px; height:48px; display:flex; align-items:center; justify-content:center; background:white; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.08); flex-shrink:0;">${this.selectedRewardIcon || '🎁'}</div>
                            <div style="flex:1;">
                                <div style="font-size:13px; font-weight:900; color:#111827; line-height:1.2;">${this.redeemDescription.toUpperCase()}</div>
                                <div style="font-size:10px; color:#f59e0b; font-weight:700; margin-top:3px;">${this.selectedRewardPoints.toLocaleString()} pts / item</div>
                            </div>
                            <div style="text-align:right; flex-shrink:0;">
                                <div style="font-size:11px; color:#6b7280; font-weight:700;">x ${this.redeemQuantity}</div>
                            </div>
                        </div>
                    </div>

                    <!-- Breakdown Harga -->
                    <div style="padding:0 24px 16px; background:white;">
                        <div style="font-size:9px; color:#6b7280; font-weight:700; text-transform:uppercase; letter-spacing:1.5px; margin-bottom:10px;">Rincian Biaya</div>
                        <div style="background:#f9fafb; border-radius:12px; overflow:hidden;">
                            <div style="display:flex; justify-content:space-between; padding:10px 14px; border-bottom:1px solid #f3f4f6;">
                                <span style="font-size:11px; color:#6b7280; font-weight:600;">Harga Barang (${this.redeemQuantity}x)</span>
                                <span style="font-size:11px; color:#ef4444; font-weight:800;">- ${itemPoints.toLocaleString()} pts</span>
                            </div>
                            ${ongkir > 0 ? `
                            <div style="display:flex; justify-content:space-between; padding:10px 14px; border-bottom:1px solid #f3f4f6;">
                                <span style="font-size:11px; color:#6b7280; font-weight:600;">🚚 Biaya Ongkir</span>
                                <span style="font-size:11px; color:#ef4444; font-weight:800;">- ${ongkir} pts</span>
                            </div>` : `
                            <div style="display:flex; justify-content:space-between; padding:10px 14px; border-bottom:1px solid #f3f4f6;">
                                <span style="font-size:11px; color:#6b7280; font-weight:600;">🏪 Ambil Sendiri</span>
                                <span style="font-size:11px; color:#10b981; font-weight:800;">Gratis</span>
                            </div>`}
                            <div style="display:flex; justify-content:space-between; padding:12px 14px; background:linear-gradient(135deg,#fffbeb,#fef3c7);">
                                <span style="font-size:12px; color:#92400e; font-weight:900; text-transform:uppercase; letter-spacing:1px;">Total</span>
                                <span style="font-size:14px; color:#f59e0b; font-weight:900;">- ${totalPoints.toLocaleString()} pts</span>
                            </div>
                        </div>
                    </div>

                    <!-- Metode & Saldo -->
                    <div style="padding:0 24px 16px; background:white; display:grid; grid-template-columns:1fr 1fr; gap:10px;">
                        <div style="background:#f0fdf4; border-radius:10px; padding:10px 12px;">
                            <div style="font-size:9px; color:#166534; font-weight:700; text-transform:uppercase; letter-spacing:1px; margin-bottom:4px;">Metode</div>
                            <div style="font-size:11px; color:#166534; font-weight:800;">${methodText}</div>
                        </div>
                        <div style="background:#fef2f2; border-radius:10px; padding:10px 12px;">
                            <div style="font-size:9px; color:#991b1b; font-weight:700; text-transform:uppercase; letter-spacing:1px; margin-bottom:4px;">Sisa Saldo</div>
                            <div style="font-size:13px; color:#dc2626; font-weight:900;">${(window.citizenServerData.availablePoints - totalPoints).toLocaleString()} pts</div>
                        </div>
                    </div>

                    <!-- Footer -->
                    <div style="position:relative; padding:0 24px; height:16px; background:white;">
                        <div style="position:absolute; left:0; right:0; top:50%; border-top:2px dashed #e5e7eb;"></div>
                        <div style="position:absolute; left:-12px; top:0; width:24px; height:24px; background:#f3f4f6; border-radius:50%;"></div>
                        <div style="position:absolute; right:-12px; top:0; width:24px; height:24px; background:#f3f4f6; border-radius:50%;"></div>
                    </div>
                    <div style="background:white; padding:14px 24px 4px; text-align:center; border-radius:0 0 12px 12px;">
                        <div style="font-size:9px; color:#9ca3af; font-weight:600;">Terima kasih telah menukarkan poin Anda 🌿</div>
                        <div style="font-size:9px; color:#d1d5db; margin-top:2px;">NetraSphere — Smart Community Waste System</div>
                    </div>
                </div>`;

            Swal.fire({
                html: receiptHtml,
                showCancelButton: true,
                confirmButtonColor: '#f59e0b',
                cancelButtonColor: '#9ca3af',
                confirmButtonText: '✅ Ya, Tukar Sekarang!',
                cancelButtonText: 'Batal',
                width: '420px',
                padding: '0',
                borderRadius: '16px',
                customClass: {
                    popup: 'receipt-popup',
                    confirmButton: 'receipt-confirm-btn',
                    cancelButton: 'receipt-cancel-btn',
                    actions: 'receipt-actions'
                }
            }).then((result) => {
                if (result.isConfirmed) {
                    this.redeemPoints = totalPoints;
                    this.redeemDescription = this.redeemQuantity + 'x ' + this.redeemDescription + ' (' + (this.fulfillmentMethod === 'delivery' ? 'Kirim ke Rumah' : 'Ambil di Eco-Center') + ')';
                    
                    // Delay submit slightly to allow x-model to sync
                    setTimeout(() => {
                        this.$refs.redeemForm.submit();
                    }, 100);
                }
            });
        },

        confirmDeposit() {
            const categorySelected = this.$refs.depositForm.querySelector('input[name="categoryId"]:checked');
            const weightInput = this.$refs.depositForm.querySelector('input[name="weightKg"]');
            const fileInput = this.$refs.depositForm.querySelector('input[type="file"]');

            if (!categorySelected) {
                Swal.fire({ icon: 'warning', title: 'Pilih Kategori', text: 'Silakan pilih kategori sampah terlebih dahulu.', confirmButtonColor: '#10b981' });
                return;
            }
            if (!weightInput.value || weightInput.value <= 0) {
                Swal.fire({ icon: 'warning', title: 'Berat Kosong', text: 'Harap masukkan berat sampah yang valid.', confirmButtonColor: '#10b981' });
                return;
            }
            if (!fileInput.files.length) {
                Swal.fire({ icon: 'warning', title: 'Foto Wajib', text: 'Harap lampirkan foto bukti sampah untuk validasi petugas.', confirmButtonColor: '#10b981' });
                return;
            }

            Swal.fire({
                title: 'Konfirmasi Setoran',
                text: 'Pastikan data yang Anda masukkan sudah benar.',
                icon: 'info',
                showCancelButton: true,
                confirmButtonColor: '#10b981',
                cancelButtonColor: '#9ca3af',
                confirmButtonText: 'Ya, Setor Sekarang!',
                cancelButtonText: 'Batal'
            }).then((result) => {
                if (result.isConfirmed) {
                    this.$refs.depositForm.submit();
                }
            });
        },

        shareImpact() {
            Swal.fire({
                title: 'Bagikan Dampak Hijau!',
                text: 'Ajak temanmu untuk ikut melestarikan lingkungan bersama NetraSphere.',
                showConfirmButton: false,
                showCloseButton: true,
                html: `
                    <div class="flex justify-center gap-6 py-6">
                        <button type="button" onclick="window.open('https://wa.me/?text=Saya%20sudah%20berkontribusi%20melestarikan%20lingkungan%20di%20NetraSphere!%20Ayo%20bergabung!', '_blank')" class="group flex flex-col items-center gap-2">
                            <div class="w-14 h-14 bg-[#25D366] text-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                                <svg class="w-7 h-7 fill-current" viewBox="0 0 24 24"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>
                            </div>
                            <span class="text-[10px] font-bold text-gray-500 uppercase tracking-wider">WhatsApp</span>
                        </button>
                        <button type="button" onclick="window.open('https://www.facebook.com/sharer/sharer.php?u=https://netrasphere.id', '_blank')" class="group flex flex-col items-center gap-2">
                            <div class="w-14 h-14 bg-[#1877F2] text-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                                <svg class="w-7 h-7 fill-current" viewBox="0 0 24 24"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
                            </div>
                            <span class="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Facebook</span>
                        </button>
                        <button type="button" onclick="window.open('https://twitter.com/intent/tweet?text=Ayo%20buat%20bumi%20lebih%20hijau%20bersama%20NetraSphere!', '_blank')" class="group flex flex-col items-center gap-2">
                            <div class="w-14 h-14 bg-black text-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                                <svg class="w-6 h-6 fill-current" viewBox="0 0 24 24"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
                            </div>
                            <span class="text-[10px] font-bold text-gray-500 uppercase tracking-wider">X / Twitter</span>
                        </button>
                        <button type="button" onclick="window.open('https://instagram.com', '_blank')" class="group flex flex-col items-center gap-2">
                            <div class="w-14 h-14 bg-gradient-to-tr from-[#f09433] via-[#dc2743] to-[#bc1888] text-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                                <svg class="w-7 h-7 fill-current" viewBox="0 0 24 24"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/></svg>
                            </div>
                            <span class="text-[10px] font-bold text-gray-500 uppercase tracking-wider">Instagram</span>
                        </button>
                    </div>
                `
            });
        },

        async sendChat() {
            if (!this.chatInput.trim()) return;
            const msg = this.chatInput;
            this.chatMessages.push({ role: 'user', content: msg });
            this.chatInput = '';
            this.chatLoading = true;
            try {
                const res = await fetch('/api/v1/chat/anonymous', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: msg })
                });
                const data = await res.json();
                this.chatMessages.push({ role: 'ai', content: data.data?.message || 'Maaf, terjadi kesalahan.' });
            } catch(e) {
                this.chatMessages.push({ role: 'ai', content: 'Maaf, layanan AI sedang tidak tersedia.' });
            }
            this.chatLoading = false;
            this.$nextTick(() => { const el = this.$refs.chatBox; if(el) el.scrollTop = el.scrollHeight; });
        },

        // INTERNAL CHAT SYSTEM
        async fetchChatUsers() {
            try {
                const res = await fetch('/internal/chat/users');
                if (res.ok) {
                    this.chatUsers = await res.json();
                }
            } catch(e) { console.error("CHAT DEBUG: Error fetching users:", e); }
        },

        async selectChatUser(user) {
            this.selectedChatUser = user;
            await this.fetchChatHistory();
            this.fetchChatUsers(); // Refresh badges in list
            this.fetchUnreadChatCount(); // Refresh main floating badge
            this.$nextTick(() => { this.scrollToBottom(); });
            if(!this.chatPollInterval) {
                this.chatPollInterval = setInterval(() => {
                    if(this.selectedChatUser && this.chatOpen) this.fetchChatHistory();
                }, 5000);
            }
        },

        async fetchChatHistory() {
            if(!this.selectedChatUser) return;
            try {
                const res = await fetch('/internal/chat/history/' + this.selectedChatUser.id);
                this.chatHistory = await res.json();
            } catch(e) { console.error(e); }
        },

        async sendChatMessage() {
            if(!this.chatMessageInput.trim() || !this.selectedChatUser) return;
            const payload = { receiverId: this.selectedChatUser.id, message: this.chatMessageInput };
            try {
                const res = await fetch('/internal/chat/send', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                if(res.ok) {
                    this.chatMessageInput = '';
                    await this.fetchChatHistory();
                    this.$nextTick(() => { this.scrollToBottom(); });
                }
            } catch(e) { console.error(e); }
        },

        async fetchUnreadChatCount() {
            try {
                const res = await fetch('/internal/chat/unread');
                const data = await res.json();
                this.unreadChatCount = data.unreadCount;
            } catch(e) {}
        },

        scrollToBottom() {
            const el = this.$refs.internalChatBox;
            if(el) el.scrollTop = el.scrollHeight;
        },

        // --- NEW FEATURES FOR ECO-CENTER ---
        
        // Snap & Sort Quiz
        snapSortQuiz() {
            return {
                current: 0,
                score: 0,
                isFinished: false,
                questions: [
                    { name: 'Botol Kaca Sirup', icon: '🍷', category: 'anorganik' },
                    { name: 'Sisa Sayur Bayam', icon: '🥬', category: 'organik' },
                    { name: 'Baterai Remote TV', icon: '🔋', category: 'b3' },
                    { name: 'Kardus Paket Belanja', icon: '📦', category: 'anorganik' },
                    { name: 'Cangkang Telur', icon: '🥚', category: 'organik' },
                    { name: 'Lampu LED Mati', icon: '💡', category: 'b3' }
                ],
                check(choice) {
                    if (choice === this.questions[this.current].category) {
                        this.score += 20;
                        Swal.fire({
                            toast: true,
                            position: 'top-end',
                            icon: 'success',
                            title: 'Benar!',
                            showConfirmButton: false,
                            timer: 1000
                        });
                    } else {
                        Swal.fire({
                            toast: true,
                            position: 'top-end',
                            icon: 'error',
                            title: 'Oops, Salah!',
                            showConfirmButton: false,
                            timer: 1000
                        });
                    }
                    
                    if (this.current < this.questions.length - 1) {
                        this.current++;
                    } else {
                        this.isFinished = true;
                    }
                },
                reset() {
                    this.current = 0;
                    this.score = 0;
                    this.isFinished = false;
                }
            };
        },

        // Eco Glossary
        ecoGlossary() {
            return {
                isOpen: false,
                current: 0,
                items: [
                    { term: 'Upcycling', definition: 'Proses kreatif mengubah barang bekas menjadi produk baru yang memiliki nilai estetika atau fungsional lebih tinggi.' },
                    { term: 'Komposting', definition: 'Proses penguraian bahan organik secara biologis menjadi humus (kompos) yang menyuburkan tanah.' },
                    { term: 'Jejak Karbon', definition: 'Total emisi gas rumah kaca yang dihasilkan oleh individu, organisasi, atau produk dalam jangka waktu tertentu.' },
                    { term: 'Zero Waste', definition: 'Filosofi gaya hidup untuk meminimalkan produksi sampah agar tidak ada yang berakhir di TPA atau lingkungan.' },
                    { term: 'B3', definition: 'Bahan Berbahaya dan Beracun yang memerlukan penanganan khusus agar tidak mencemari lingkungan.' },
                    { term: 'Biodegradable', definition: 'Bahan yang dapat terurai secara alami oleh mikroorganisme dalam waktu yang relatif singkat.' }
                ],
                open(index) {
                    this.current = index;
                    this.isOpen = true;
                }
            };
        }
    }));
});

document.addEventListener('DOMContentLoaded', function() {
    // Initialize Map
    const mapElement = document.getElementById('smartBinMap');
    if (mapElement) {
        const map = L.map('smartBinMap').setView([-6.8961, 107.6356], 14);

        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);

        // ── Icon Factories ──────────────────────────────────────
        function binIcon(color) {
            return L.divIcon({
                className: '',
                html: `<div style="background:${color};color:white;width:34px;height:34px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:17px;border:2.5px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.25);">🤖</div>`,
                iconSize: [34, 34],
                iconAnchor: [17, 17],
                popupAnchor: [0, -18]
            });
        }

        function carIcon(color) {
            return L.divIcon({
                className: '',
                html: `<div style="background:${color};color:white;width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:19px;border:2.5px solid white;box-shadow:0 3px 10px rgba(0,0,0,0.3);">🚛</div>`,
                iconSize: [36, 36],
                iconAnchor: [18, 18],
                popupAnchor: [0, -20]
            });
        }

        // ── NetraDUMP Locations (Dynamic from Server + Fallback)
        let bins = [];
        const serverBins = window.citizenServerData.smartBins || [];
        
        if (serverBins.length > 0) {
            bins = serverBins.map(b => ({
                lat: b.latitude,
                lng: b.longitude,
                name: b.name + " (" + b.location + ")",
                fill: b.fillLevel,
                status: b.active ? (b.fillLevel >= 90 ? "Penuh" : "Aktif") : "Offline",
                color: !b.active ? "#9ca3af" : (b.fillLevel >= 90 ? "#ef4444" : (b.fillLevel >= 70 ? "#f59e0b" : "#10b981"))
            }));
        } else {
            // Fallback for demo
            bins = [
                { lat: -6.8961, lng: 107.6356, name: "NetraDUMP #01 — Kampus ITENAS",       fill: 85,  status: "Penuh",   color: "#ef4444" },
                { lat: -6.8990, lng: 107.6320, name: "NetraDUMP #02 — Pahlawan",            fill: 40,  status: "Aktif",   color: "#10b981" },
                { lat: -6.8930, lng: 107.6380, name: "NetraDUMP #03 — Cikutra",             fill: 60,  status: "Aktif",   color: "#f59e0b" },
                { lat: -6.9020, lng: 107.6330, name: "NetraDUMP #04 — Suci (PHH Mustofa)",  fill: 95,  status: "Penuh",   color: "#ef4444" },
                { lat: -6.9000, lng: 107.6250, name: "NetraDUMP #05 — Surapati",            fill: 20,  status: "Aktif",   color: "#10b981" }
            ];
        }

        bins.forEach(bin => {
            const fillBar = `<div style="background:#e5e7eb;border-radius:4px;height:7px;margin-top:5px;overflow:hidden;"><div style="background:${bin.color};height:7px;width:${bin.fill}%;border-radius:4px;"></div></div>`;
            const statusBadge = `<span style="background:${bin.color};color:white;padding:1px 8px;border-radius:9999px;font-size:11px;font-weight:700;">${bin.status}</span>`;
            L.marker([bin.lat, bin.lng], { icon: binIcon(bin.color) })
                .addTo(map)
                .bindPopup(
                    `<div style="font-family:sans-serif;min-width:160px;">
                        <p style="font-weight:800;font-size:13px;margin-bottom:4px;">${bin.name}</p>
                        ${statusBadge}
                        ${bin.fill >= 0 ? `<p style="font-size:11px;color:#6b7280;margin-top:6px;margin-bottom:2px;">Kapasitas: <b>${bin.fill}%</b></p>${fillBar}` : ''}
                     </div>`
                );
        });

        // ── Armada Petugas (10 locations around ITENAS)
        const vehicles = [
            { lat: -6.8983, lng: 107.6364, name: "Armada P-01 — Pak Budi",      route: "Rute: ITENAS → Suci",           status: "Mengangkut",       color: "#8b5cf6" },
            { lat: -6.8950, lng: 107.6300, name: "Armada P-02 — Bu Sari",       route: "Rute: Pahlawan → Cikutra",      status: "Dalam Perjalanan", color: "#3b82f6" },
            { lat: -6.8900, lng: 107.6300, name: "Armada P-03 — Pak Agus",      route: "Rute: Cikutra Barat → Dago",    status: "Dalam Perjalanan", color: "#3b82f6" },
            { lat: -6.9010, lng: 107.6400, name: "Armada P-04 — Bu Dewi",       route: "Rute: Suci Timur → Cicaheum",   status: "Standby",          color: "#64748b" },
            { lat: -6.9030, lng: 107.6500, name: "Armada P-05 — Pak Joko",      route: "Rute: Cicaheum → Antapani",     status: "Mengangkut",       color: "#8b5cf6" },
            { lat: -6.9100, lng: 107.6500, name: "Armada P-06 — Pak Yanto",     route: "Rute: Terusan Jakarta",         status: "Dalam Perjalanan", color: "#3b82f6" },
            { lat: -6.9060, lng: 107.6250, name: "Armada P-07 — Bu Ratna",      route: "Rute: WR Supratman",            status: "Standby",          color: "#64748b" },
            { lat: -6.9013, lng: 107.6204, name: "Armada P-08 — Pak Rahmat",    route: "Rute: Diponegoro → Gasibu",     status: "Mengangkut",       color: "#8b5cf6" },
            { lat: -6.8800, lng: 107.6150, name: "Armada P-09 — Pak Slamet",    route: "Rute: Dago Atas → Tubagus",     status: "Dalam Perjalanan", color: "#3b82f6" },
            { lat: -6.8880, lng: 107.6250, name: "Armada P-10 — Bu Lina",       route: "Rute: Sadang Serang → Cikutra", status: "Standby",          color: "#64748b" },
        ];

        vehicles.forEach(v => {
            const statusColor = v.status === "Standby" ? "#64748b" : v.status === "Mengangkut" ? "#8b5cf6" : "#3b82f6";
            const statusBadge = `<span style="background:${statusColor};color:white;padding:1px 8px;border-radius:9999px;font-size:11px;font-weight:700;">${v.status}</span>`;
            L.marker([v.lat, v.lng], { icon: carIcon(v.color) })
                .addTo(map)
                .bindPopup(
                    `<div style="font-family:sans-serif;min-width:170px;">
                        <p style="font-weight:800;font-size:13px;margin-bottom:4px;">${v.name}</p>
                        ${statusBadge}
                        <p style="font-size:11px;color:#6b7280;margin-top:6px;">${v.route}</p>
                     </div>`
                );
        });
    }

    // Initialize B3 Drop-Off Map
    const b3MapElement = document.getElementById('b3DropOffMap');
    if (b3MapElement) {
        const b3Map = L.map('b3DropOffMap').setView([-6.8961, 107.6356], 14);
        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; OSM contributors'
        }).addTo(b3Map);

        const b3Locations = [
            { lat: -6.8961, lng: 107.6356, name: "Drop-Off Utama ITENAS" },
            { lat: -6.9020, lng: 107.6330, name: "Pusat B3 Suci" },
            { lat: -6.9150, lng: 107.6520, name: "Hub Antapani" }
        ];

        b3Locations.forEach(loc => {
            L.marker([loc.lat, loc.lng], {
                icon: L.divIcon({
                    className: '',
                    html: `<div style="background:#ef4444;color:white;width:24px;height:24px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:12px;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.2);">⚠️</div>`,
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                })
            }).addTo(b3Map).bindPopup(`<b>${loc.name}</b><br>Siap menerima limbah B3.`);
        });
    }

    // Initialize Charts if data is available
    if (window.citizenServerData && window.citizenServerData.ecoStats) {
        const stats = window.citizenServerData.ecoStats;
        
        // Category Pie Chart
        const catCanvas = document.getElementById('categoryChart');
        if (catCanvas && stats.weightByCategory) {
            const labels = Object.keys(stats.weightByCategory);
            const data = Object.values(stats.weightByCategory);
            if (labels.length > 0) {
                new Chart(catCanvas, {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            data: data,
                            backgroundColor: ['#10b981', '#f59e0b', '#3b82f6', '#ef4444', '#8b5cf6'],
                            borderWidth: 2,
                            borderColor: '#ffffff'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8 } }
                        },
                        cutout: '65%'
                    }
                });
            } else {
                catCanvas.parentElement.innerHTML = '<div class="flex h-full items-center justify-center text-gray-400 text-sm">Belum ada data setoran.</div>';
            }
        }

        // Trend Line Chart
        const trendCanvas = document.getElementById('trendChart');
        if (trendCanvas && stats.trendMonths && stats.trendWeights) {
            if (stats.trendMonths.length > 0) {
                new Chart(trendCanvas, {
                    type: 'line',
                    data: {
                        labels: stats.trendMonths,
                        datasets: [{
                            label: 'Berat (Kg)',
                            data: stats.trendWeights,
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.1)',
                            borderWidth: 3,
                            fill: true,
                            tension: 0.4,
                            pointBackgroundColor: '#ffffff',
                            pointBorderColor: '#10b981',
                            pointBorderWidth: 2,
                            pointRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: { beginAtZero: true, grid: { borderDash: [4, 4] } },
                            x: { grid: { display: false } }
                        }
                    }
                });
            } else {
                trendCanvas.parentElement.innerHTML = '<div class="flex h-full items-center justify-center text-gray-400 text-sm">Belum ada data setoran bulanan.</div>';
            }
        }
    }
});
