package com.smartwaste.service;

import com.smartwaste.entity.RewardItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface untuk manajemen Katalog Hadiah (Reward Catalog).
 *
 * <p><b>OOP Concept — Interface:</b> Mendefinisikan kontrak operasi reward catalog.
 * Implementasi dipisahkan di {@code RewardItemServiceImpl}.</p>
 */
public interface RewardItemService {

    /** Semua reward (Admin view) */
    Page<RewardItem> getAll(Pageable pageable);

    /** Semua reward aktif (Citizen view) */
    List<RewardItem> getAllActive();

    /** Detail satu reward */
    RewardItem getById(String id);

    /** Admin buat reward baru */
    RewardItem create(String name, String description, String icon, double pointsCost, int stock, String requiredLevel, boolean isPopular);

    /** Admin update reward */
    RewardItem update(String id, String name, String description, String icon, double pointsCost, int stock, String requiredLevel, boolean isPopular);

    /** Toggle aktif/nonaktif reward */
    void toggleActive(String id);

    /** Hapus reward (hanya jika belum pernah ditukar) */
    void delete(String id);

    /** Kurangi stok saat redemption di-approve */
    void decreaseStock(String id);
}
