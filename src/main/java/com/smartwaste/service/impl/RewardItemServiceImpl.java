package com.smartwaste.service.impl;

import com.smartwaste.entity.RewardItem;
import com.smartwaste.repository.RewardItemRepository;
import com.smartwaste.service.RewardItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementasi {@link RewardItemService} — Manajemen Katalog Hadiah.
 *
 * <p><b>OOP Concept — Encapsulation:</b> Semua logika bisnis katalog hadiah
 * dienkapsulasi di sini, tersembunyi dari controller dan layer lain.</p>
 */
@Service
@RequiredArgsConstructor
public class RewardItemServiceImpl implements RewardItemService {

    private final RewardItemRepository rewardItemRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RewardItem> getAll(Pageable pageable) {
        return rewardItemRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardItem> getAllActive() {
        return rewardItemRepository.findAllByActiveTrueOrderByPointsCostAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public RewardItem getById(String id) {
        return rewardItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reward tidak ditemukan: " + id));
    }

    @Override
    @Transactional
    public RewardItem create(String name, String description, String icon, double pointsCost, int stock, String requiredLevel, boolean isPopular) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nama reward tidak boleh kosong.");
        if (pointsCost <= 0) throw new IllegalArgumentException("Biaya poin harus lebih dari 0.");
        RewardItem item = new RewardItem(name.trim(), description, icon, pointsCost, stock, requiredLevel, isPopular, false);
        return rewardItemRepository.save(item);
    }

    @Override
    @Transactional
    public RewardItem update(String id, String name, String description, String icon, double pointsCost, int stock, String requiredLevel, boolean isPopular) {
        RewardItem item = getById(id);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nama reward tidak boleh kosong.");
        if (pointsCost <= 0) throw new IllegalArgumentException("Biaya poin harus lebih dari 0.");
        item.setName(name.trim());
        item.setDescription(description);
        item.setIcon(icon != null && !icon.isBlank() ? icon : "🎁");
        item.setPointsCost(pointsCost);
        item.setStock(stock);
        item.setRequiredLevel(requiredLevel != null ? requiredLevel : "Green Starter");
        item.setPopular(isPopular);
        return rewardItemRepository.save(item);
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        RewardItem item = getById(id);
        item.setActive(!item.isActive());
        rewardItemRepository.save(item);
    }

    @Override
    @Transactional
    public void delete(String id) {
        RewardItem item = getById(id);
        long usageCount = rewardItemRepository.countRedemptionsByRewardItemId(id);
        if (usageCount > 0) {
            throw new IllegalStateException(
                "Reward '" + item.getName() + "' tidak bisa dihapus karena sudah pernah ditukar oleh warga (" + usageCount + " transaksi).");
        }
        rewardItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void decreaseStock(String id) {
        if (id == null || id.isBlank()) return;
        rewardItemRepository.findById(id).ifPresent(item -> {
            item.decreaseStock();
            rewardItemRepository.save(item);
        });
    }
}
