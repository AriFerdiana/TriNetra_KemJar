package com.smartwaste.service;

import com.smartwaste.dto.response.WalletResponse;
import com.smartwaste.entity.PointRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface untuk manajemen Green Wallet dan Penukaran Poin.
 *
 * <p><b>OOP Concept — Interface:</b>
 * Mendefinisikan kontrak operasi wallet dan redemption.
 * Implementasi ({@code GreenWalletServiceImpl}) terpisah dari kontrak ini.</p>
 */
public interface GreenWalletService {

    /** Lihat saldo wallet milik citizen yang sedang login */
    WalletResponse getMyWallet(String citizenEmail);

    /** Lihat wallet berdasarkan citizenId (Admin only) */
    WalletResponse getWalletByCitizenId(String citizenId);

    /**
     * Ajukan permintaan penukaran poin — status PENDING (perlu admin approve).
     * Poin BELUM dikurangi sampai admin approve.
     */
    PointRedemption requestRedemption(String citizenEmail, double points, String description, String rewardItemId);

    /** Admin menyetujui penukaran poin → kurangi poin dari wallet */
    WalletResponse approveRedemption(String redemptionId, String adminNotes);

    /** Admin menolak penukaran poin → poin tetap di wallet */
    WalletResponse rejectRedemption(String redemptionId, String adminNotes);

    /** Daftar semua redemption pending (untuk admin) */
    Page<PointRedemption> getPendingRedemptions(Pageable pageable);

    /** Semua redemption (semua status, untuk admin) */
    Page<PointRedemption> getAllRedemptions(String search, Pageable pageable);

    /** Riwayat redemption milik citizen */
    Page<PointRedemption> getMyRedemptions(String citizenEmail, Pageable pageable);

    void updateTargetPoints(String citizenEmail, Double targetPoints);
}
