function collectorDashboard() {
    const serverData = window.collectorServerData || {};
    const mapData = serverData.mapData || [];
    const categoryStats = serverData.categoryStats || [];

    return {
        activeTab: 'dashboard',
        isSidebarCollapsed: localStorage.getItem('collector_sidebar_collapsed') === 'true',
        showQRScanner: false,
        showReportIssueModal: false,
        currentGeoLocation: '',
        html5QrCode: null,
        lastPendingCount: 0,
        smartBins: [],
        chatSearchQuery: '',
        open: false,
        chatInitialized: false,
        chartInitialized: false,
        mapInitialized: false,
        currentLoadKg: serverData.currentLoad || 0,
        vehicleCapacityKg: serverData.maxCapacity || 500,
        filterCategory: 'ALL',
        darkMode: localStorage.getItem('darkMode') === 'true',
        notification: { 
            show: serverData.notificationShow || false, 
            message: serverData.notificationMessage || '', 
            type: serverData.notificationType || '' 
        },
        lastKnownPendingCount: serverData.pendingCount || 0,
        showPasswordModal: false,
        showEditProfileModal: false,
        profileMenuOpen: false,
        showManualDepositModal: false,
        showConfirmDepositModal: false,
        showRejectModal: false,
        confirmingDeposit: null,
        rejectingDepositId: null,
        rejectReason: '',
        manualDepositForm: { citizenId: '', categoryId: '', weightKg: '', notes: '' },
        editProfileForm: { 
            name: serverData.collectorName || '', 
            phone: serverData.phone || '', 
            vehicleNumber: serverData.vehicleNumber || '',
            maxCapacityKg: serverData.maxCapacity || 500
        },
        citizenSearchResults: [],
        searchCitizenQuery: '',
        isSearching: false,
        lightbox: { show: false, img: '' },
        pickupProofUrl: '',

        // Internal Chat State
        chatOpen: false,
        chatUsers: [],
        selectedChatUser: null,
        chatHistory: [],
        chatMessageInput: '',
        unreadChatCount: 0,
        
        available: serverData.available || false,

        async init() {
            this.initChart();
            // Init dashboard mini-map after short delay to ensure DOM is rendered
            setTimeout(() => this.initMiniMap(), 200);
            this.fetchUnreadChatCount();
            
            // Set initial lastPendingCount from server-side data if available
            const pendingCountEl = document.querySelector('[data-pending-count]');
            if(pendingCountEl) {
                this.lastPendingCount = parseInt(pendingCountEl.getAttribute('data-pending-count')) || 0;
            }
            
            // Start real-time polling every 10 seconds
            setInterval(() => this.checkNewTasks(), 10000);
            
            // Get current geolocation for reporting
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(pos => {
                    this.currentGeoLocation = `${pos.coords.latitude},${pos.coords.longitude}`;
                });
            }
            
            if(this.darkMode) document.body.classList.add('dark-mode');
            if(this.notification.show) {
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    icon: this.notification.type,
                    title: this.notification.message,
                    showConfirmButton: false,
                    timer: 4000,
                    timerProgressBar: true
                });
            }
            this.startPolling();
            setInterval(() => this.fetchUnreadChatCount(), 10000);
            
            // Auto-init charts after data load if visible
            setTimeout(() => {
                if(this.activeTab === 'stats') this.initChart();
            }, 500);

            // Offline Sync Initialization
            this.checkOfflineStorage();
        },

        checkOfflineStorage() {
            const offlineData = JSON.parse(localStorage.getItem('offline_deposits') || '[]');
            if (offlineData.length > 0) {
                Swal.fire({
                    title: 'Sinkronisasi Data?',
                    text: `Terdapat ${offlineData.length} data offline yang belum tersimpan.`,
                    icon: 'info',
                    showCancelButton: true,
                    confirmButtonText: 'Sinkronkan Sekarang'
                }).then(async (result) => {
                    if (result.isConfirmed) {
                        for (const item of offlineData) {
                            await this.syncOfflineItem(item);
                        }
                        localStorage.removeItem('offline_deposits');
                        Swal.fire('Berhasil!', 'Semua data telah tersinkronisasi.', 'success');
                    }
                });
            }
        },

        async syncOfflineItem(item) {
            try {
                await fetch('/collector/deposit/manual', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/x-www-form-urlencoded',
                        ...this.getCsrfHeaders() 
                    },
                    body: new URLSearchParams(item)
                });
            } catch (e) { console.error("Sync failed for", item, e); }
        },
        
        toggleSidebar() {
            this.isSidebarCollapsed = !this.isSidebarCollapsed;
            localStorage.setItem('sidebarCollapsed', this.isSidebarCollapsed);
            // Trigger map resize if collapsing/expanding
            setTimeout(() => {
                window.dispatchEvent(new Event('resize'));
            }, 300);
        },

        toggleDarkMode() {
            this.darkMode = !this.darkMode;
            localStorage.setItem('darkMode', this.darkMode);
            document.body.classList.toggle('dark-mode');
        },

        async fetchChatUsers() {
            try {
                const res = await fetch('/internal/chat/users');
                if (res.ok) {
                    this.chatUsers = await res.json();
                    console.log("CHAT DEBUG: Loaded users:", this.chatUsers);
                } else {
                    console.error("CHAT DEBUG: Failed to fetch users, status:", res.status);
                }
            } catch(e) {
                console.error("CHAT DEBUG: Error fetching users:", e);
            }
        },

        get filteredChatUsers() {
            if (!this.chatSearchQuery) return this.chatUsers;
            const q = this.chatSearchQuery.toLowerCase();
            return this.chatUsers.filter(u => 
                (u.name && u.name.toLowerCase().includes(q)) || 
                (u.email && u.email.toLowerCase().includes(q)) ||
                (u.role && u.role.toLowerCase().includes(q))
            );
        },

        async selectChatUser(user) {
            this.selectedChatUser = user;
            await this.fetchChatHistory();
            this.fetchChatUsers(); // Refresh badges in list
            this.fetchUnreadChatCount(); // Refresh main floating badge
            this.$nextTick(() => { this.scrollToBottom(); });
            // Poll for new messages every 5 seconds if chat is open
            if(!this.chatPollInterval) {
                this.chatPollInterval = setInterval(() => {
                    if(this.selectedChatUser) this.fetchChatHistory();
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

        simulatePhotoCapture() {
            const randomId = Math.floor(Math.random() * 1000);
            this.pickupProofUrl = `https://picsum.photos/seed/${randomId}/600/400`;
            Swal.fire({
                icon: 'success',
                title: 'Foto Berhasil Diambil!',
                text: 'Bukti penjemputan telah tersimpan.',
                timer: 1500,
                showConfirmButton: false
            });
        },

        confirmDepositWithPhoto(deposit) {
            this.confirmingDeposit = deposit;
            this.pickupProofUrl = ''; 
            this.showConfirmDepositModal = true;
        },

        openRejectModal(id) {
            this.rejectingDepositId = id;
            this.rejectReason = '';
            this.showRejectModal = true;
        },

        async submitReject() {
            if(!this.rejectReason.trim()) {
                Swal.fire({ icon: 'warning', title: 'Mohon isi alasan penolakan' });
                return;
            }
            try {
                const res = await fetch(`/collector/deposit/${this.rejectingDepositId}/reject`, {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/x-www-form-urlencoded',
                        ...this.getCsrfHeaders() 
                    },
                    body: new URLSearchParams({ reason: this.rejectReason })
                });
                if(res.ok) {
                    window.location.reload();
                }
            } catch(e) { console.error(e); }
        },

        getCsrfHeaders() {
            const token = document.querySelector('meta[name="_csrf"]')?.content || '';
            const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
            return { [header]: token };
        },

        async fetchUnreadChatCount() {
            try {
                const res = await fetch('/internal/chat/unread');
                const data = await res.json();
                this.unreadChatCount = data.unreadCount;
            } catch(e) {}
        },

        scrollToBottom() {
            const el = this.$refs.chatBox;
            if(el) el.scrollTop = el.scrollHeight;
        },
        
        async initMiniMap() {
            const el = document.getElementById('map');
            if (!el || el._leaflet_id) return; // already initialized
            const map = L.map('map', { zoomControl: true, scrollWheelZoom: false })
                         .setView([-6.8975, 107.6350], 14);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);
            setTimeout(() => map.invalidateSize(), 300);

            // Add pending deposit markers
            mapData.forEach(d => {
                if(d.location) {
                    const coords = d.location.split(',');
                    if(coords.length === 2 && !isNaN(coords[0])) {
                        L.marker([parseFloat(coords[0]), parseFloat(coords[1])], {
                            icon: L.divIcon({
                                className: 'custom-div-icon',
                                html: `<div style="background:#10b981;width:22px;height:22px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,.2);display:flex;align-items:center;justify-content:center;font-size:10px;">🏠</div>`,
                                iconSize: [22, 22], iconAnchor: [11, 11]
                            })
                        }).addTo(map)
                         .bindPopup(`<b>${d.citizenName}</b><br>${d.categoryName} (${d.weightKg}kg)`);
                    }
                }
            });

            // Smart Bins on mini-map
            try {
                const res = await fetch('/collector/smart-bins');
                const bins = await res.json();
                bins.forEach(bin => {
                    const color = bin.fillLevel > 90 ? '#ef4444' : (bin.fillLevel > 70 ? '#f97316' : '#10b981');
                    L.circleMarker([bin.latitude, bin.longitude], { radius: 7, color: '#fff', fillColor: color, fillOpacity: 1, weight: 2 })
                     .addTo(map)
                     .bindPopup(`<b>${bin.name}</b><br>Kapasitas: ${bin.fillLevel}%`);
                });
            } catch(e) {}
        },

        async initFullMap() {
            const el = document.getElementById('fullMap');
            if (!el || el._leaflet_id) return; // already initialized
            const map = L.map('fullMap').setView([-6.8975, 107.6350], 15);
            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);
            setTimeout(() => map.invalidateSize(), 300);

            // Add Deposit Markers
            mapData.forEach(d => {
                if(d.location) {
                    const coords = d.location.split(',');
                    if(coords.length === 2 && !isNaN(coords[0])) {
                        L.marker([parseFloat(coords[0]), parseFloat(coords[1])], {
                            icon: L.divIcon({
                                className: 'custom-div-icon',
                                html: `<div class='marker-pin bg-blue-500 shadow-lg border-2 border-white w-7 h-7 rounded-full flex items-center justify-center text-[10px] text-white font-bold'>🏠</div>`,
                                iconSize: [30, 42], iconAnchor: [15, 42]
                            })
                        }).addTo(map)
                         .bindPopup(`<b>Warga: ${d.citizenName}</b><br>${d.categoryName} (${d.weightKg}kg)<br><a href='https://www.google.com/maps?q=${d.location}' target='_blank' style='color:#3b82f6;font-weight:bold;'>Navigate ➔</a>`);
                    }
                }
            });

            // Fetch and Add Smart Bins
            try {
                const res = await fetch('/collector/smart-bins');
                const bins = await res.json();
                bins.forEach(bin => {
                    const color = bin.fillLevel > 90 ? 'bg-red-500' : (bin.fillLevel > 70 ? 'bg-orange-500' : 'bg-emerald-500');
                    L.marker([bin.latitude, bin.longitude], {
                        icon: L.divIcon({
                            className: 'bin-icon',
                            html: `<div class='${color} p-1 rounded-lg border-2 border-white shadow-xl flex flex-col items-center'>
                                        <span style='font-size:14px'>🗑️</span>
                                        <span style='font-size:8px;color:white;font-weight:900;line-height:1'>${bin.fillLevel}%</span>
                                   </div>`,
                            iconSize: [32, 32]
                        })
                    }).addTo(map)
                     .bindPopup(`<b>${bin.name}</b><br>Kapasitas: ${bin.fillLevel}%<br>Status: ${bin.status}`);
                });
            } catch(e) { console.error("Error loading smart bins", e); }

            this.$watch('activeTab', (value) => {
                if (value === 'map') {
                    setTimeout(() => map.invalidateSize(), 200);
                }
            });
        },


        // QR SCANNER LOGIC
        startQRScanner() {
            this.showQRScanner = true;
            this.$nextTick(() => {
                this.html5QrCode = new Html5Qrcode("reader");
                const config = { fps: 10, qrbox: { width: 250, height: 250 } };
                this.html5QrCode.start(
                    { facingMode: "environment" }, 
                    config, 
                    (decodedText) => {
                        let citizenId = decodedText;
                        if (decodedText.includes('/citizen/')) {
                            citizenId = decodedText.split('/').pop();
                        }
                        this.searchCitizenQuery = citizenId;
                        this.manualDepositForm.citizenId = citizenId;
                        this.stopQRScanner();
                        this.searchCitizens(); 
                    },
                    (errorMessage) => { /* ignore */ }
                ).catch(err => {
                    console.error("QR Error", err);
                    Swal.fire({ icon: 'error', title: 'Gagal akses kamera', text: err });
                    this.showQRScanner = false;
                });
            });
        },

        stopQRScanner() {
            if (this.html5QrCode) {
                this.html5QrCode.stop().then(() => {
                    this.html5QrCode.clear();
                    this.showQRScanner = false;
                });
            } else {
                this.showQRScanner = false;
            }
        },

        async checkNewTasks() {
            if(!this.available) return;
            try {
                const res = await fetch('/collector/pending-count');
                const data = await res.json();
                if(data.count > this.lastPendingCount) {
                    this.lastPendingCount = data.count;
                    this.playNotificationSound();
                    Swal.fire({
                        title: 'Tugas Baru!',
                        text: 'Ada setoran sampah baru yang menunggu konfirmasi.',
                        icon: 'info',
                        toast: true,
                        position: 'top-end',
                        showConfirmButton: false,
                        timer: 5000,
                        timerProgressBar: true,
                        didOpen: (toast) => {
                            toast.addEventListener('mouseenter', Swal.stopTimer)
                            toast.addEventListener('mouseleave', Swal.resumeTimer)
                            toast.onclick = () => { this.activeTab = 'pending'; Swal.close(); };
                        }
                    });
                }
            } catch(e) {}
        },

        playNotificationSound() {
            try {
                const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                const oscillator = audioCtx.createOscillator();
                const gainNode = audioCtx.createGain();
                oscillator.connect(gainNode);
                gainNode.connect(audioCtx.destination);
                oscillator.type = 'sine';
                oscillator.frequency.setValueAtTime(880, audioCtx.currentTime); // A5
                gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime);
                oscillator.start();
                oscillator.stop(audioCtx.currentTime + 0.2);
            } catch(e) { console.warn("Audio notification failed", e); }
        },

        sortByDistance() {
            if (!navigator.geolocation) {
                Swal.fire('Error', 'Browser tidak mendukung geolokasi', 'error');
                return;
            }
            Swal.fire({
                title: 'Menghitung Jarak...',
                allowOutsideClick: false,
                didOpen: () => Swal.showLoading()
            });

            navigator.geolocation.getCurrentPosition(pos => {
                const myLat = pos.coords.latitude;
                const myLon = pos.coords.longitude;

                const container = document.querySelector('.pending-tasks-container');
                if(!container) {
                    Swal.fire('Info', 'Tidak ada tugas untuk diurutkan', 'info');
                    return;
                }

                const cards = Array.from(container.children);
                
                cards.sort((a, b) => {
                    const locA = a.getAttribute('data-location')?.split(',');
                    const locB = b.getAttribute('data-location')?.split(',');
                    
                    if(!locA || locA.length < 2) return 1;
                    if(!locB || locB.length < 2) return -1;

                    const distA = this.calculateDistance(myLat, myLon, parseFloat(locA[0]), parseFloat(locA[1]));
                    const distB = this.calculateDistance(myLat, myLon, parseFloat(locB[0]), parseFloat(locB[1]));
                    
                    // Update distance labels
                    const labelA = a.querySelector('.distance-badge');
                    if(labelA) {
                        labelA.innerText = distA < 1 ? Math.round(distA * 1000) + ' m' : distA.toFixed(1) + ' km';
                        labelA.classList.remove('hidden');
                    }
                    const labelB = b.querySelector('.distance-badge');
                    if(labelB) {
                        labelB.innerText = distB < 1 ? Math.round(distB * 1000) + ' m' : distB.toFixed(1) + ' km';
                        labelB.classList.remove('hidden');
                    }

                    return distA - distB;
                });

                // Re-append sorted cards
                cards.forEach(card => container.appendChild(card));

                Swal.fire({
                    icon: 'success',
                    title: 'Terurut!',
                    text: 'Daftar telah diurutkan berdasarkan lokasi terdekat Anda.',
                    timer: 1500,
                    showConfirmButton: false
                });
            }, err => {
                Swal.fire('Error', 'Gagal mendapatkan lokasi: ' + err.message, 'error');
            });
        },

        calculateDistance(lat1, lon1, lat2, lon2) {
            const R = 6371; // Earth radius in km
            const dLat = (lat2 - lat1) * Math.PI / 180;
            const dLon = (lon2 - lon1) * Math.PI / 180;
            const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
                      Math.sin(dLon/2) * Math.sin(dLon/2);
            const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return R * c;
        },

        async searchCitizens() {
            if (this.searchCitizenQuery.length < 3) {
                this.citizenSearchResults = [];
                return;
            }
            try {
                const response = await fetch('/collector/search-citizens?q=' + encodeURIComponent(this.searchCitizenQuery));
                this.citizenSearchResults = await response.json();
            } catch (error) { console.error(error); }
        },

        selectCitizen(citizen) {
            this.manualDepositForm.citizenId = citizen.id;
            this.searchCitizenQuery = citizen.name;
            this.citizenSearchResults = [];
        },

        submitManualDeposit() {
            if (!this.manualDepositForm.citizenId || !this.manualDepositForm.categoryId || !this.manualDepositForm.weightKg) {
                Swal.fire({ icon: 'warning', title: 'Data belum lengkap!' });
                return;
            }
            document.getElementById('manualDepositRealForm').submit();
        },

        async markRead(id, element) {
            try {
                const res = await fetch('/collector/notifications/' + id + '/read', {
                    method: 'POST',
                    headers: { ...this.getCsrfHeaders() }
                });
                if (res.ok) {
                    element.classList.remove('bg-emerald-50/10');
                    const dot = element.querySelector('.w-2.h-2.rounded-full.bg-emerald-500');
                    if(dot) dot.remove();
                }
            } catch (error) { console.error(error); }
        },

        startPolling() {
            setInterval(async () => {
                try {
                    const res = await fetch('/collector/pending-count');
                    if (res.ok) {
                        const data = await res.json();
                        if (data.count > this.lastKnownPendingCount) {
                            this.lastKnownPendingCount = data.count;
                            this.playNotificationSound();
                            Swal.fire({
                                title: 'Ada Aktivitas Baru',
                                text: 'Data dashboard telah diperbarui dari server.',
                                icon: 'success',
                                toast: true,
                                position: 'top-end',
                                showConfirmButton: true,
                                confirmButtonText: 'Segarkan',
                                timer: 6000
                            }).then(result => {
                                if(result.isConfirmed) window.location.reload();
                            });
                        }
                    }
                } catch(e) {}
            }, 30000);
        },

        initChart() {
            if (this.chartInitialized) return;
            if (!categoryStats || categoryStats.length === 0) return;
            this.chartInitialized = true;
            
            const labels = categoryStats.map(s => s[0]);
            const weights = categoryStats.map(s => s[1]);
            const counts = categoryStats.map(s => s[2]);
            
            // Premium Multi-Color Palette
            const colors = [
                '#10B981', '#3B82F6', '#F59E0B', '#8B5CF6', '#EC4899', 
                '#06B6D4', '#F43F5E', '#6366F1', '#84CC16', '#F97316'
            ];

            if (document.getElementById('weightChart')) {
                new Chart(document.getElementById('weightChart'), {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{ 
                            data: weights, 
                            backgroundColor: colors, 
                            borderWidth: 0,
                            hoverOffset: 15
                        }]
                    },
                    options: { 
                        responsive: true, 
                        maintainAspectRatio: false,
                        cutout: '80%', 
                        plugins: { 
                            legend: { 
                                position: 'bottom',
                                labels: {
                                    usePointStyle: true,
                                    padding: 15,
                                    font: { size: 10, weight: 'bold', family: 'Inter' }
                                }
                            },
                            tooltip: {
                                backgroundColor: '#1e293b',
                                callbacks: { label: (item) => ` ${item.label}: ${item.raw.toFixed(1)} kg` }
                            }
                        } 
                    }
                });
            }

            if (document.getElementById('countChart')) {
                new Chart(document.getElementById('countChart'), {
                    type: 'bar',
                    data: {
                        labels: labels,
                        datasets: [{ 
                            label: 'Setoran', 
                            data: counts, 
                            backgroundColor: colors,
                            borderRadius: 8
                        }]
                    },
                    options: { 
                        responsive: true, 
                        maintainAspectRatio: false,
                        plugins: { legend: { display: false } }, 
                        scales: { 
                            y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.03)' } },
                            x: { 
                                grid: { display: false },
                                ticks: { font: { size: 8, weight: 'bold' }, maxRotation: 45, minRotation: 45 }
                            }
                        } 
                    }
                });
            }

            // Trend Chart
            const trendData = serverData.collectionTrend || [];
            if (document.getElementById('trendChart') && trendData.length > 0) {
                new Chart(document.getElementById('trendChart'), {
                    type: 'line',
                    data: {
                        labels: trendData.map(d => d.date),
                        datasets: [{
                            label: 'Berat (kg)',
                            data: trendData.map(d => d.weight),
                            borderColor: '#10b981',
                            borderWidth: 4,
                            backgroundColor: 'rgba(16, 185, 129, 0.05)',
                            fill: true,
                            tension: 0.4,
                            pointRadius: 6,
                            pointHoverRadius: 8,
                            pointBackgroundColor: '#ffffff',
                            pointBorderColor: '#10b981',
                            pointBorderWidth: 3
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)', drawBorder: false } },
                            x: { grid: { display: false }, ticks: { font: { weight: 'bold' } } }
                        }
                    }
                });
            }
        },

        endShiftAndLogout() {
            Swal.fire({
                title: 'Selesaikan Shift?',
                text: "Anda akan keluar dari sesi kerja.",
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#10b981',
                confirmButtonText: 'Ya, Logout'
            }).then((result) => {
                if (result.isConfirmed) document.getElementById('formLogout').submit();
            });
        },

        async toggleStatusWithSummary() {
            try {
                const res = await fetch('/collector/toggle-availability', {
                    method: 'POST',
                    headers: { ...this.getCsrfHeaders() }
                });
                if (res.ok) {
                    this.available = !this.available;
                }
            } catch (error) { console.error(error); }
        },

        async resetLoad() {
            const result = await Swal.fire({
                title: 'Kosongkan Muatan?',
                text: "Data muatan kendaraan akan di-reset ke 0.",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#ef4444',
                confirmButtonText: 'Ya, Kosongkan'
            });
            
            if (result.isConfirmed) {
                try {
                    const res = await fetch('/collector/reset-load', {
                        method: 'POST',
                        headers: { ...this.getCsrfHeaders() }
                    });
                    if (res.ok) {
                        this.currentLoadKg = 0; 
                        Swal.fire({
                            toast: true,
                            position: 'top-end',
                            icon: 'success',
                            title: 'Muatan telah dikosongkan',
                            showConfirmButton: false,
                            timer: 3000
                        });
                    }
                } catch (error) { console.error(error); }
            }
        }
    };
}
