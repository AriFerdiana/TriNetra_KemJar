package com.smartwaste.repository;

import com.smartwaste.entity.RewardItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository untuk entitas {@link RewardItem}.
 */
@Repository
public interface RewardItemRepository extends JpaRepository<RewardItem, String> {

    /** Semua reward aktif (untuk ditampilkan ke warga) */
    List<RewardItem> findAllByActiveTrueOrderByPointsCostAsc();

    /** Semua reward (untuk admin), urut terbaru */
    @org.springframework.lang.NonNull
    Page<RewardItem> findAll(@org.springframework.lang.NonNull Pageable pageable);

    /** Cek apakah ada redemption yang menggunakan reward ini */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(r) FROM PointRedemption r WHERE r.rewardItemId = :rewardItemId")
    long countRedemptionsByRewardItemId(@org.springframework.data.repository.query.Param("rewardItemId") String rewardItemId);
}
