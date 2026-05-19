package com.smartwaste.service;

import com.smartwaste.dto.request.RegisterCollectorRequest;
import com.smartwaste.dto.response.CollectorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface untuk manajemen Collector (Petugas Pengumpul Sampah).
 *
 * <p><b>OOP Concept — Interface:</b> Memisahkan kontrak dari implementasi.</p>
 */
public interface CollectorService {

    /** Daftarkan petugas baru (Admin only) */
    CollectorResponse registerCollector(RegisterCollectorRequest request);

    /** Daftar semua petugas (aktif) */
    Page<CollectorResponse> getAllCollectors(Pageable pageable);

    /** Cari petugas berdasarkan keyword */
    Page<CollectorResponse> searchCollectors(String keyword, Boolean active, Pageable pageable);

    /** Detail satu petugas */
    CollectorResponse getById(String id);

    /** Update data petugas */
    CollectorResponse updateCollector(String id, String name, String phone,
                                      String vehicleNumber, String assignedArea);

    /** Aktifkan/nonaktifkan akun petugas */
    void toggleActive(String id);

    /** Update ketersediaan petugas */
    CollectorResponse setAvailability(String collectorEmail, boolean available);

    /** Total petugas aktif */
    long countActive();

    void resetPassword(String collectorId, String newPassword);
}
