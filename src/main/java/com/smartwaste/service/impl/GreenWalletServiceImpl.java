package com.smartwaste.service.impl;

import com.smartwaste.dto.response.WalletResponse;
import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.GreenWallet;
import com.smartwaste.entity.PointRedemption;
import com.smartwaste.entity.enums.RedemptionStatus;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.GreenWalletRepository;
import com.smartwaste.repository.PointRedemptionRepository;
import com.smartwaste.repository.RewardItemRepository;
import com.smartwaste.entity.RewardItem;
import com.smartwaste.service.GreenWalletService;
import com.smartwaste.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementasi service Green Wallet dengan alur approval yang benar.
 *
 * <p><b>OOP — Encapsulation:</b> Logika penambahan/pengurangan poin
 * dilakukan via method di entity ({@link GreenWallet#addPoints(double)} dan
 * {@link GreenWallet#redeemPoints(double)}), bukan langsung mengubah field.</p>
 *
 * <p><b>Alur Redemption yang Benar:</b>
 * <pre>
 *   Citizen request → PointRedemption[PENDING] → Admin review
 *       ├── approve → poin dikurangi dari wallet → [APPROVED]
 *       └── reject  → poin tetap utuh → [REJECTED]
 * </pre>
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class GreenWalletServiceImpl implements GreenWalletService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GreenWalletServiceImpl.class);

    private final CitizenRepository citizenRepository;
    private final GreenWalletRepository walletRepository;
    private final PointRedemptionRepository redemptionRepository;
    private final RewardItemRepository rewardItemRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public GreenWalletServiceImpl(CitizenRepository citizenRepository,
                                  GreenWalletRepository walletRepository,
                                  PointRedemptionRepository redemptionRepository,
                                  RewardItemRepository rewardItemRepository,
                                  NotificationService notificationService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.citizenRepository = citizenRepository;
        this.walletRepository = walletRepository;
        this.redemptionRepository = redemptionRepository;
        this.rewardItemRepository = rewardItemRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public WalletResponse getMyWallet(String citizenEmail) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        GreenWallet wallet = walletRepository.findByCitizen(citizen)
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenEmail", citizenEmail));
        return mapToResponse(wallet);
    }

    @Override
    public WalletResponse getWalletByCitizenId(String citizenId) {
        GreenWallet wallet = walletRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenId", citizenId));
        return mapToResponse(wallet);
    }

    /**
     * Mengajukan permintaan penukaran poin — POIN BELUM DIKURANGI.
     * Status PENDING sampai admin approve/reject.
     */
    @Override
    @Transactional
    public PointRedemption requestRedemption(String citizenEmail, double points, String description, String rewardItemId) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        GreenWallet wallet = walletRepository.findByCitizen(citizen)
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenEmail", citizenEmail));

        double actualPoints = points;
        String actualDescription = description;

        // [PATCH BUG KRITIS] Validasi harga asli & stok langsung dari database
        if (rewardItemId != null && !rewardItemId.isBlank()) {
            RewardItem rewardItem = rewardItemRepository.findById(rewardItemId)
                    .orElseThrow(() -> new ResourceNotFoundException("RewardItem", "id", rewardItemId));
            
            // Validasi ketersediaan
            if (!rewardItem.isAvailable()) {
                throw new IllegalStateException("Hadiah '" + rewardItem.getName() + "' saat ini tidak tersedia atau stok habis.");
            }

            // Override input dari frontend dengan data asli database
            actualPoints = rewardItem.getPointsCost();
            actualDescription = rewardItem.getName();
            
            // Note: Tambahan pengecekan Tier/Level bisa ditambahkan di sini berdasarkan totalPoints wallet
        }

        // Validasi saldo cukup sebelum request (berdasarkan poin asli)
        if (actualPoints > wallet.getAvailablePoints()) {
            throw new com.smartwaste.exception.InsufficientPointsException(actualPoints, wallet.getAvailablePoints());
        }
        if (actualPoints <= 0) {
            throw new IllegalArgumentException("Jumlah poin harus lebih dari 0.");
        }

        // Buat redemption request dengan status PENDING menggunakan poin & deskripsi asli
        PointRedemption redemption = new PointRedemption(citizen, actualPoints, actualDescription);
        redemption.setRewardItemId(rewardItemId); // Link ke katalog hadiah jika ada
        PointRedemption saved = redemptionRepository.save(redemption);
        log.info("Permintaan penukaran {} poin (Asli: {}) oleh {} — status PENDING", points, actualPoints, citizenEmail);
        return saved;
    }

    /**
     * Admin menyetujui penukaran — poin dikurangi sekarang.
     */
    @Override
    @Transactional
    public WalletResponse approveRedemption(String redemptionId, String adminNotes) {
        PointRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new ResourceNotFoundException("PointRedemption", "id", redemptionId));

        if (redemption.getStatus() != RedemptionStatus.PENDING) {
            throw new IllegalStateException("Hanya penukaran berstatus PENDING yang bisa disetujui.");
        }

        GreenWallet wallet = walletRepository.findByCitizen(redemption.getCitizen())
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenId", redemption.getCitizen().getId()));

        // --- Validasi dan Kurangi Stok Reward Item ---
        if (redemption.getRewardItemId() != null && !redemption.getRewardItemId().isBlank()) {
            RewardItem rewardItem = rewardItemRepository.findById(redemption.getRewardItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("RewardItem", "id", redemption.getRewardItemId()));
            
            if (!rewardItem.isAvailable()) {
                throw new IllegalStateException("Stok hadiah '" + rewardItem.getName() + "' sudah habis atau tidak aktif.");
            }
            rewardItem.decreaseStock();
            rewardItemRepository.save(rewardItem);
        }

        // Kurangi poin dari wallet (encapsulation: validasi di dalam entity)
        wallet.redeemPoints(redemption.getPointsRedeemed());
        walletRepository.save(wallet);

        // Update status redemption
        redemption.setStatus(RedemptionStatus.APPROVED);
        redemption.setAdminNotes(adminNotes);
        redemption.setRewardCode("RWD-" + System.currentTimeMillis());
        redemptionRepository.save(redemption);

        log.info("Penukaran {} poin untuk {} disetujui.",
                redemption.getPointsRedeemed(), redemption.getCitizen().getEmail());

        notificationService.sendNotification(
            redemption.getCitizen(),
            "Penukaran Berhasil!",
            String.format("Penukaran %.0f poin untuk %s telah disetujui. Kode: %s", redemption.getPointsRedeemed(), redemption.getDescription(), redemption.getRewardCode()),
            "SUCCESS"
        );

        // Broadcast to citizen that their wallet points updated
        messagingTemplate.convertAndSend("/queue/citizen/" + redemption.getCitizen().getEmail() + "/wallet", "POINTS_UPDATED");

        return mapToResponse(wallet);
    }

    /**
     * Admin menolak penukaran — poin tetap utuh di wallet.
     */
    @Override
    @Transactional
    public WalletResponse rejectRedemption(String redemptionId, String adminNotes) {
        PointRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new ResourceNotFoundException("PointRedemption", "id", redemptionId));

        if (redemption.getStatus() != RedemptionStatus.PENDING) {
            throw new IllegalStateException("Hanya penukaran berstatus PENDING yang bisa ditolak.");
        }

        redemption.setStatus(RedemptionStatus.REJECTED);
        redemption.setAdminNotes(adminNotes != null ? adminNotes : "Ditolak oleh admin.");
        redemptionRepository.save(redemption);

        log.info("Penukaran {} poin untuk {} ditolak.",
                redemption.getPointsRedeemed(), redemption.getCitizen().getEmail());

        notificationService.sendNotification(
            redemption.getCitizen(),
            "Penukaran Ditolak",
            String.format("Penukaran %.0f poin untuk %s ditolak. Saldo dikembalikan. Alasan: %s", redemption.getPointsRedeemed(), redemption.getDescription(), adminNotes),
            "WARNING"
        );

        GreenWallet wallet = walletRepository.findByCitizen(redemption.getCitizen())
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenId", redemption.getCitizen().getId()));
        return mapToResponse(wallet);
    }

    @Override
    public Page<PointRedemption> getPendingRedemptions(Pageable pageable) {
        return redemptionRepository.findByStatus(RedemptionStatus.PENDING, pageable);
    }

    @Override
    public Page<PointRedemption> getAllRedemptions(String search, Pageable pageable) {
        return redemptionRepository.findWithSearch(search, pageable);
    }

    @Override
    public Page<PointRedemption> getMyRedemptions(String citizenEmail, Pageable pageable) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        return redemptionRepository.findByCitizen(citizen, pageable);
    }

    @Override
    @Transactional
    public void updateTargetPoints(String citizenEmail, Double targetPoints) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        GreenWallet wallet = walletRepository.findByCitizen(citizen)
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenEmail", citizenEmail));
        
        wallet.setTargetPoints(targetPoints != null && targetPoints > 0 ? targetPoints : 1000.0);
        walletRepository.save(wallet);
    }

    private WalletResponse mapToResponse(GreenWallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .citizenId(wallet.getCitizen().getId())
                .citizenName(wallet.getCitizen().getName())
                .totalPoints(wallet.getTotalPoints())
                .redeemedPoints(wallet.getRedeemedPoints())
                .availablePoints(wallet.getAvailablePoints())
                .targetPoints(wallet.getTargetPoints() != null ? wallet.getTargetPoints() : 1000.0)
                .build();
    }
}
