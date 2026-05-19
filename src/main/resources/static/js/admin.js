function adminDashboard() {
    const urlParams = new URLSearchParams(window.location.search);

    return {
        sidebarOpen: window.innerWidth >= 1024,
        isSidebarCollapsed: false,
        activeTab: urlParams.get('activeTab') || 'overview',

        // ── Modal visibility flags ──────────────────────────
        showAddCategoryModal:        false,
        showEditCategoryModal:       false,
        showImportModal:             false,
        showPasswordModal:           false,
        showRegisterCollectorModal:  false,
        showApproveModal:            false,
        showRejectModal:             false,
        showCitizenDetailModal:      false,
        showDeleteCategoryModal:     false,
        showAddRewardModal:          false,
        showEditRewardModal:         false,
        showDeleteRewardModal:       false,
        showAddArticleModal:         false,
        showEditArticleModal:        false,
        showAddFaqModal:             false,
        showEditFaqModal:            false,
        showDeleteFaqModal:          false,
        showBroadcastModal:          false,
        showResolveReportModal:      false,

        // ── Custom confirm modals (pengganti browser confirm()) ──
        showDeleteDepositModal:          false,
        showResetCitizenPasswordModal:   false,
        showResetCollectorPasswordModal: false,
        showDeleteArticleModal:          false,

        // ── Data state untuk custom confirm ──
        pendingDeleteDepositForm:   null,
        pendingResetCitizenForm:    null,
        pendingResetCollectorForm:  null,
        pendingDeleteArticleForm:   null,

        // ── Form data state ──────────────────────────────────
        editArticleData:         { id: '', title: '', content: '', type: 'NEWS', externalImageUrl: '', author: 'Admin' },
        editFaqData:             { id: '', question: '', answer: '', displayOrder: 0 },
        selectedDeleteFaq:       { id: '', question: '' },
        selectedRedemptionId:    '',
        adminNotes:              '',
        rejectNotes:             '',
        editingCategory:         { id:'', name:'', description:'', pointsPerKg:0 },
        selectedDeleteCategory:  { id: '', name: '' },
        editingReward:           { id: '', name: '', description: '', icon: '🎁', pointsCost: 0, stock: -1, requiredLevel: 'Green Starter', isPopular: false },
        selectedDeleteReward:    { id: '', name: '' },
        selectedCitizen:         { name:'', email:'', nik:'', phone:'', address:'', totalDeposits:0, totalPoints:0, availablePoints:0 },
        selectedReport:          { id: '', title: '', collector: '' },

        // ── Helper Methods ───────────────────────────────────
        openCitizenDetail(name, email, nik, phone, address, totalDeposits, totalPoints, availablePoints) {
            this.selectedCitizen = { name, email, nik, phone, address, totalDeposits, totalPoints, availablePoints };
            this.showCitizenDetailModal = true;
        },

        openApproveModal(id) {
            this.selectedRedemptionId = id;
            this.adminNotes = '';
            this.showApproveModal = true;
        },

        openRejectModal(id) {
            this.selectedRedemptionId = id;
            this.rejectNotes = '';
            this.showRejectModal = true;
        },

        openEditCategory(id, name, desc, pts) {
            this.editingCategory = { id, name, description: desc, pointsPerKg: pts };
            this.showEditCategoryModal = true;
        },

        openDeleteCategory(id, name) {
            this.selectedDeleteCategory = { id, name };
            this.showDeleteCategoryModal = true;
        },

        openEditReward(id, name, desc, icon, pts, stock, reqLevel, popular) {
            this.editingReward = { id, name, description: desc, icon, pointsCost: pts, stock, requiredLevel: reqLevel, isPopular: popular };
            this.showEditRewardModal = true;
        },

        openDeleteReward(id, name) {
            this.selectedDeleteReward = { id, name };
            this.showDeleteRewardModal = true;
        },

        openEditArticle(id, title, content, type, extimg, author) {
            this.editArticleData = { id, title, content, type, externalImageUrl: extimg, author };
            this.showEditArticleModal = true;
        },

        openEditFaq(id, question, answer, displayOrder) {
            this.editFaqData = { id, question, answer, displayOrder };
            this.showEditFaqModal = true;
        },

        openDeleteFaq(id, question) {
            this.selectedDeleteFaq = { id, question };
            this.showDeleteFaqModal = true;
        },

        openResolveReport(id, title, collector) {
            this.selectedReport = { id, title, collector };
            this.showResolveReportModal = true;
        },

        // ── Custom Confirm Modal Handlers ────────────────────

        /** Panggil ini dari tombol hapus setoran, simpan reference form-nya */
        confirmDeleteDeposit(formEl) {
            this.pendingDeleteDepositForm = formEl;
            this.showDeleteDepositModal = true;
        },

        /** Eksekusi submit form hapus setoran setelah user konfirmasi */
        executeDeleteDeposit() {
            if (this.pendingDeleteDepositForm) {
                this.pendingDeleteDepositForm.submit();
            }
            this.showDeleteDepositModal = false;
        },

        /** Panggil ini dari tombol reset password warga */
        confirmResetCitizenPassword(formEl) {
            this.pendingResetCitizenForm = formEl;
            this.showResetCitizenPasswordModal = true;
        },

        executeResetCitizenPassword() {
            if (this.pendingResetCitizenForm) {
                this.pendingResetCitizenForm.submit();
            }
            this.showResetCitizenPasswordModal = false;
        },

        /** Panggil ini dari tombol reset password petugas */
        confirmResetCollectorPassword(formEl) {
            this.pendingResetCollectorForm = formEl;
            this.showResetCollectorPasswordModal = true;
        },

        executeResetCollectorPassword() {
            if (this.pendingResetCollectorForm) {
                this.pendingResetCollectorForm.submit();
            }
            this.showResetCollectorPasswordModal = false;
        },

        /** Panggil ini dari tombol hapus artikel */
        confirmDeleteArticle(formEl) {
            this.pendingDeleteArticleForm = formEl;
            this.showDeleteArticleModal = true;
        },

        executeDeleteArticle() {
            if (this.pendingDeleteArticleForm) {
                this.pendingDeleteArticleForm.submit();
            }
            this.showDeleteArticleModal = false;
        },

        toggleSidebar() {
            this.isSidebarCollapsed = !this.isSidebarCollapsed;
        },

        // ── Tab Title ───────────────────────────────────────
        tabTitle() {
            const titles = {
                overview:    'Ringkasan Dashboard',
                citizens:    'Manajemen Warga',
                deposits:    'Setoran Sampah',
                categories:  'Kategori Sampah',
                collectors:  'Manajemen Petugas',
                redemptions: 'Persetujuan Penukaran',
                rewards:     'Katalog Hadiah',
                articles:    'Manajemen Berita',
                faqs:        'Manajemen FAQ',
                reports:     'Laporan Lapangan',
                iot:         'Monitoring IoT',
                logs:        'Log Sistem'
            };
            return titles[this.activeTab] || 'Dashboard';
        },

        // ── IoT Helpers ─────────────────────────────────────
        /** Kembalikan class CSS berdasarkan fill level */
        fillLevelClass(level) {
            if (level >= 80) return 'fill-high';
            if (level >= 50) return 'fill-medium';
            return 'fill-low';
        },

        fillLevelLabel(level) {
            if (level >= 80) return '🔴 Hampir Penuh';
            if (level >= 50) return '🟡 Sedang';
            return '🟢 Normal';
        }
    };
}

// ── Initialize Charts after DOM loaded ──────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
    const monthlyStats  = window.adminServerData?.monthlyStats || [];
    const sortedStats   = Array.isArray(monthlyStats)
        ? [...monthlyStats].sort((a, b) => (a.monthYear || '').localeCompare(b.monthYear || ''))
        : [];
    const labels        = sortedStats.map(s => s.monthYear || '');
    const depositCounts = sortedStats.map(s => s.depositCount || 0);
    const weights       = sortedStats.map(s => parseFloat((s.totalWeightKg || 0).toFixed(2)));

    // Register ChartDataLabels plugin globally if available
    if (typeof Chart !== 'undefined' && typeof ChartDataLabels !== 'undefined') {
        Chart.register(ChartDataLabels);
    }

    // Calculate insights and badges
    let totalCount = depositCounts.reduce((a, b) => a + b, 0);
    let totalWeight = weights.reduce((a, b) => a + b, 0);

    const depositsBadge = document.getElementById('totalDepositsBadge');
    if (depositsBadge) depositsBadge.textContent = totalCount + ' Setoran';

    const weightBadge = document.getElementById('totalWeightBadge');
    if (weightBadge) weightBadge.textContent = totalWeight.toFixed(1) + ' kg';

    // Deposit Count Insight
    let depositInsightText = "Frekuensi setoran warga stabil bulan ini.";
    if (depositCounts.length >= 2) {
        let lastVal = depositCounts[depositCounts.length - 1];
        let prevVal = depositCounts[depositCounts.length - 2];
        if (prevVal > 0) {
            let percentDiff = Math.round(((lastVal - prevVal) / prevVal) * 100);
            if (percentDiff > 0) {
                depositInsightText = `Jumlah setoran meningkat ${percentDiff}% dibanding bulan sebelumnya.`;
            } else if (percentDiff < 0) {
                depositInsightText = `Jumlah setoran menurun ${Math.abs(percentDiff)}% dibanding bulan sebelumnya.`;
            } else {
                depositInsightText = "Jumlah setoran stabil dibanding bulan sebelumnya.";
            }
        }
    }
    const depositInsightEl = document.getElementById('depositInsightText');
    if (depositInsightEl) depositInsightEl.textContent = depositInsightText;

    // Weight Insight
    let weightInsightText = "Volume berat sampah terkelola stabil.";
    if (weights.length >= 2) {
        let lastVal = weights[weights.length - 1];
        let prevVal = weights[weights.length - 2];
        if (prevVal > 0) {
            let percentDiff = Math.round(((lastVal - prevVal) / prevVal) * 100);
            if (percentDiff > 0) {
                weightInsightText = `Total berat sampah meningkat ${percentDiff}% dibanding bulan lalu.`;
            } else if (percentDiff < 0) {
                weightInsightText = `Total berat sampah berkurang ${Math.abs(percentDiff)}% dibanding bulan lalu.`;
            } else {
                weightInsightText = "Volume sampah stabil dibanding bulan lalu.";
            }
        }
    }
    const weightInsightEl = document.getElementById('weightInsightText');
    if (weightInsightEl) weightInsightEl.textContent = weightInsightText;

    // ── Chart 1: Statistik Jumlah Setoran (Bar Chart) ──────────────────
    const ctxCount = document.getElementById('depositCountChart');
    if (ctxCount && typeof Chart !== 'undefined') {
        const gradBar = ctxCount.getContext('2d').createLinearGradient(0, 0, 0, 200);
        gradBar.addColorStop(0, 'rgba(99, 102, 241, 0.85)');
        gradBar.addColorStop(1, 'rgba(99, 102, 241, 0.1)');

        new Chart(ctxCount, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Jumlah Setoran',
                    data: depositCounts,
                    backgroundColor: gradBar,
                    borderColor: '#6366f1',
                    borderWidth: 1.5,
                    borderRadius: 6,
                    borderSkipped: false,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: 'rgba(17, 24, 39, 0.95)',
                        padding: 10,
                        cornerRadius: 8,
                        titleColor: '#f9fafb',
                        bodyColor: '#d1d5db',
                        callbacks: {
                            label: function(context) {
                                return ` Jumlah: ${context.parsed.y} kali setoran`;
                            }
                        }
                    },
                    datalabels: {
                        align: 'top',
                        anchor: 'end',
                        color: '#4f46e5',
                        offset: -2,
                        font: {
                            family: 'Plus Jakarta Sans',
                            weight: 'bold',
                            size: 10
                        },
                        formatter: function(val) {
                            return val > 0 ? val : '';
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { family: 'Plus Jakarta Sans', size: 9 }, color: '#9ca3af' }
                    },
                    y: {
                        grid: { color: 'rgba(0, 0, 0, 0.03)', drawTicks: false },
                        ticks: { precision: 0, color: '#9ca3af', font: { family: 'Plus Jakarta Sans', size: 9 } }
                    }
                }
            }
        });
    }

    // ── Chart 2: Statistik Berat Total Sampah (Line Area Chart) ──────────────
    const ctxWeight = document.getElementById('depositWeightChart');
    if (ctxWeight && typeof Chart !== 'undefined') {
        const gradWeight = ctxWeight.getContext('2d').createLinearGradient(0, 0, 0, 200);
        gradWeight.addColorStop(0, 'rgba(16, 185, 129, 0.25)');
        gradWeight.addColorStop(1, 'rgba(16, 185, 129, 0.01)');

        new Chart(ctxWeight, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: 'Berat Total (kg)',
                    data: weights,
                    backgroundColor: gradWeight,
                    borderColor: '#10b981',
                    borderWidth: 2.5,
                    fill: true,
                    tension: 0.4,
                    pointBackgroundColor: '#10b981',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 1.5,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: 'rgba(17, 24, 39, 0.95)',
                        padding: 10,
                        cornerRadius: 8,
                        titleColor: '#f9fafb',
                        bodyColor: '#d1d5db',
                        callbacks: {
                            label: function(context) {
                                return ` Berat: ${context.parsed.y.toFixed(1)} kg`;
                            }
                        }
                    },
                    datalabels: {
                        align: 'top',
                        anchor: 'end',
                        color: '#059669',
                        offset: 4,
                        font: {
                            family: 'Plus Jakarta Sans',
                            weight: 'bold',
                            size: 10
                        },
                        formatter: function(val) {
                            return val > 0 ? val.toFixed(1) + ' kg' : '';
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { family: 'Plus Jakarta Sans', size: 9 }, color: '#9ca3af' }
                    },
                    y: {
                        grid: { color: 'rgba(0, 0, 0, 0.03)', drawTicks: false },
                        ticks: { color: '#9ca3af', font: { family: 'Plus Jakarta Sans', size: 9 } }
                    }
                }
            }
        });
    }

    // ── Leaflet.js Waste Hotspot Map Inisialisasi ──────────────────────────
    const mapEl = document.getElementById('hotspotMap');
    if (mapEl && typeof L !== 'undefined') {
        const smartBins = window.adminServerData?.smartBins || [];
        
        // Center default: Kota Bandung / Jakarta jika tidak ada data GPS valid
        let centerLat = -6.9175; 
        let centerLng = 107.6191;
        
        if (smartBins.length > 0) {
            let sumLat = 0;
            let sumLng = 0;
            let validCount = 0;
            smartBins.forEach(bin => {
                const lat = parseFloat(bin.latitude);
                const lng = parseFloat(bin.longitude);
                if (!isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0) {
                    sumLat += lat;
                    sumLng += lng;
                    validCount++;
                }
            });
            if (validCount > 0) {
                centerLat = sumLat / validCount;
                centerLng = sumLng / validCount;
            }
        }
        
        const map = L.map('hotspotMap', {
            center: [centerLat, centerLng],
            zoom: 13,
            zoomControl: true,
            scrollWheelZoom: false // nonaktifkan agar halaman scrolling nyaman
        });
        
        // Tile style premium "Voyager" CartoDB
        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
            subdomains: 'abcd',
            maxZoom: 20
        }).addTo(map);
        
        // Tambahkan Marker dinamis sesuai fill level
        smartBins.forEach(bin => {
            const lat = parseFloat(bin.latitude);
            const lng = parseFloat(bin.longitude);
            if (isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) return;
            
            const fillLevel = bin.fillLevel || 0;
            let color = '#10b981'; // Green
            let statusText = 'Normal';
            if (fillLevel >= 80) {
                color = '#ef4444'; // Red
                statusText = 'Hampir Penuh';
            } else if (fillLevel >= 50) {
                color = '#f59e0b'; // Amber
                statusText = 'Sedang';
            }
            
            // Marker bulat premium dengan ping efek radar
            const markerHtml = `
                <div class="relative flex items-center justify-center" style="width: 24px; height: 24px;">
                    <span class="animate-ping absolute inline-flex h-6 w-6 rounded-full opacity-40" style="background-color: ${color};"></span>
                    <div class="relative rounded-full h-4 w-4 border-2 border-white shadow-lg flex items-center justify-center" style="background-color: ${color};"></div>
                </div>
            `;
            
            const customIcon = L.divIcon({
                html: markerHtml,
                className: 'custom-leaflet-marker',
                iconSize: [24, 24],
                iconAnchor: [12, 12]
            });
            
            const marker = L.marker([lat, lng], { icon: customIcon }).addTo(map);
            
            // Format HTML popup yang sangat rapi dan cantik
            const popupContent = `
                <div class="p-3 font-sans min-w-[200px]">
                    <div class="flex items-center justify-between mb-2">
                        <span class="text-xs font-bold text-gray-400">🤖 DEVID: ${bin.deviceId || '-'}</span>
                        <span class="px-2 py-0.5 rounded-full text-[9px] font-bold" style="background-color: ${color}20; color: ${color};">
                            ${statusText}
                        </span>
                    </div>
                    <h4 class="font-bold text-gray-800 text-sm mb-1">${bin.name || 'Smart Bin'}</h4>
                    <div class="space-y-1.5 mt-2 text-xs">
                        <div class="flex justify-between items-center text-gray-500">
                            <span>Kapasitas Isi:</span>
                            <span class="font-bold text-gray-700">${fillLevel}%</span>
                        </div>
                        <div class="w-full bg-gray-100 rounded-full h-1.5 overflow-hidden">
                            <div class="h-full rounded-full" style="width: ${fillLevel}%; background-color: ${color};"></div>
                        </div>
                        <div class="flex justify-between text-gray-500 pt-1">
                            <span>Sinyal WiFi:</span>
                            <span class="font-bold text-emerald-600">⚡ Bagus</span>
                        </div>
                        <div class="flex justify-between text-gray-500">
                            <span>Baterai:</span>
                            <span class="font-bold text-emerald-600">🔋 92%</span>
                        </div>
                    </div>
                </div>
            `;
            marker.bindPopup(popupContent);
        });
    }

    // ── Chart 2: Doughnut Distribusi Berat per Kategori ──────────────────
    const catStats = window.adminServerData?.weightByCategory || {};
    const ctx3 = document.getElementById('categoryChart');
    if (ctx3) {
        const catLabels = Object.keys(catStats);
        const catData   = Object.values(catStats).map(v => parseFloat(v.toFixed(2)));
        const catColors = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
        new Chart(ctx3, {
            type: 'doughnut',
            data: {
                labels: catLabels,
                datasets: [{
                    data: catData,
                    backgroundColor: catColors,
                    borderWidth: 3,
                    borderColor: '#fff',
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                cutout: '68%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            font: { size: 11 },
                            padding: 12,
                            usePointStyle: true,
                            pointStyle: 'circle',
                            color: '#4b5563'
                        }
                    }
                }
            }
        });
    }

    // ── Chart 3: Level Distribusi Warga ──────────────────
    const levelStats = window.adminServerData?.levelDistribution || {};
    const ctx5 = document.getElementById('levelChart');
    if (ctx5) {
        const levelLabels = Object.keys(levelStats);
        const levelData   = Object.values(levelStats);
        const levelColors = ['#7c3aed', '#d97706', '#6b7280', '#92400e', '#10b981'];
        new Chart(ctx5, {
            type: 'bar',
            data: {
                labels: levelLabels,
                datasets: [{
                    label: 'Jumlah Warga',
                    data: levelData,
                    backgroundColor: levelColors,
                    borderRadius: 8,
                    borderSkipped: false
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { precision: 0, color: '#9ca3af', font: { size: 11 } }
                    },
                    y: {
                        grid: { display: false },
                        ticks: { color: '#374151', font: { size: 10 } }
                    }
                }
            }
        });
    }
});
