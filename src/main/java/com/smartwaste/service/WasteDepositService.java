package com.smartwaste.service;

import com.smartwaste.dto.request.CreateWasteDepositRequest;
import com.smartwaste.dto.request.IoTDepositRequest;
import com.smartwaste.dto.response.WasteDepositResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface untuk manajemen setoran sampah.
 */
public interface WasteDepositService {

    /** Citizen membuat setoran baru via UI */
    WasteDepositResponse createDeposit(String citizenEmail, CreateWasteDepositRequest request);

    /** Robot/Smart bin membuat setoran via IoT endpoint */
    WasteDepositResponse createIoTDeposit(IoTDepositRequest request);

    /** Collector mengkonfirmasi setoran → poin dikreditkan ke wallet */
    WasteDepositResponse confirmDeposit(String depositId, String collectorEmail, String pickupProofUrl);

    /** Collector menolak setoran */
    WasteDepositResponse rejectDeposit(String depositId, String collectorEmail, String reason);

    /** Riwayat setoran citizen sendiri */
    Page<WasteDepositResponse> getMyDeposits(String citizenEmail, Pageable pageable);

    /** Semua setoran (admin/collector view) dengan filter tanggal opsional */
    Page<WasteDepositResponse> getAllDeposits(String search, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String status, Pageable pageable);

    /** Setoran yang pending (untuk collector) */
    Page<WasteDepositResponse> getPendingDeposits(Pageable pageable);

    WasteDepositResponse getById(String depositId);

    /** Riwayat setoran untuk collector (termasuk yang confirmed dan rejected) */
    Page<WasteDepositResponse> getCollectorHistory(String collectorEmail, Pageable pageable);

    long countByCollector(com.smartwaste.entity.Collector collector);
    long countByCollectorAndStatus(com.smartwaste.entity.Collector collector, com.smartwaste.entity.enums.DepositStatus status);

    double getTodayWeightByCollector(String collectorEmail);

    /** Jumlah setoran PENDING (untuk polling auto-refresh) */
    long countPendingDeposits();

    /** Statistik kategori untuk Collector */
    java.util.List<Object[]> getCategoryStatsByCollector(String collectorEmail);

    /** Mencatat setoran manual oleh Collector secara langsung (tanpa aplikasi warga) */
    void createManualDeposit(String collectorEmail, String citizenId, String categoryId, double weightKg, String notes);

    /** Analytics: Tren koleksi 7 hari terakhir */
    java.util.List<java.util.Map<String, Object>> getCollectionTrendByCollector(String collectorEmail);

    /** Analytics: Total berat kumulatif petugas */
    double getTotalWeightByCollector(String collectorEmail);

    /** Analytics: Total poin yang disalurkan oleh petugas */
    double getTotalPointsByCollector(String collectorEmail);

    /** Analytics: Jumlah warga unik yang dilayani */
    long countUniqueCitizensServedByCollector(String collectorEmail);

    /** Riwayat setoran yang sudah dikonfirmasi oleh collector tertentu */
    java.util.List<WasteDepositResponse> getConfirmedByCollector(String collectorId);

    void deleteDeposit(String depositId);
}
