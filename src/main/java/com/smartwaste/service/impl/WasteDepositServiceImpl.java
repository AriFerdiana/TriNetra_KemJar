package com.smartwaste.service.impl;

import com.smartwaste.dto.request.CreateWasteDepositRequest;
import com.smartwaste.dto.request.IoTDepositRequest;
import com.smartwaste.dto.response.WasteDepositResponse;
import com.smartwaste.entity.*;
import com.smartwaste.entity.enums.DepositStatus;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.exception.UnauthorizedException;
import com.smartwaste.repository.*;
import com.smartwaste.service.NotificationService;
import com.smartwaste.service.WasteDepositService;
import com.smartwaste.service.AchievementService;
import com.smartwaste.strategy.PointCalculatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementasi service setoran sampah — logika bisnis terpenting dalam sistem.
 *
 * <p><b>OOP Concept — Polymorphism via Strategy Pattern:</b>
 * Method {@link #confirmDeposit} menggunakan {@link PointCalculatorContext}
 * untuk menghitung poin secara polimorfis berdasarkan tipe sampah.</p>
 */
@Service
@Transactional(readOnly = true)
public class WasteDepositServiceImpl implements WasteDepositService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WasteDepositServiceImpl.class);

    private final WasteDepositRepository depositRepository;
    private final CitizenRepository citizenRepository;
    private final CollectorRepository collectorRepository;
    private final WasteCategoryRepository categoryRepository;
    private final GreenWalletRepository walletRepository;
    private final PointCalculatorContext pointCalculatorContext; // Strategy Pattern
    private final NotificationService notificationService;
    private final AchievementService achievementService;
    private final SimpMessagingTemplate messagingTemplate;

    public WasteDepositServiceImpl(WasteDepositRepository depositRepository,
                                   CitizenRepository citizenRepository,
                                   CollectorRepository collectorRepository,
                                   WasteCategoryRepository categoryRepository,
                                   GreenWalletRepository walletRepository,
                                   PointCalculatorContext pointCalculatorContext,
                                   NotificationService notificationService,
                                   AchievementService achievementService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.depositRepository = depositRepository;
        this.citizenRepository = citizenRepository;
        this.collectorRepository = collectorRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.pointCalculatorContext = pointCalculatorContext;
        this.notificationService = notificationService;
        this.achievementService = achievementService;
        this.messagingTemplate = messagingTemplate;
    }

    @Value("${app.iot.api-key}")
    private String iotApiKey;

    // ==================== Create Deposit (Manual) ====================

    @Override
    @Transactional
    public WasteDepositResponse createDeposit(String citizenEmail, CreateWasteDepositRequest request) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        WasteCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", request.getCategoryId()));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Kategori sampah ini sudah tidak aktif.");
        }

        WasteDeposit deposit = new WasteDeposit(citizen, category, request.getWeightKg(), request.getNotes());
        deposit.setImageUrl(request.getImageUrl());

        WasteDeposit saved = depositRepository.save(deposit);
        log.info("Setoran baru dibuat oleh {} - kategori: {}, berat: {} kg",
                citizenEmail, category.getName(), request.getWeightKg());

        // Broadcast to collectors that there is a new pending deposit
        messagingTemplate.convertAndSend("/topic/deposits", "NEW_DEPOSIT");

        return mapToResponse(saved);
    }

    // ==================== Create Deposit (IoT) ====================

    @Override
    @Transactional
    public WasteDepositResponse createIoTDeposit(IoTDepositRequest request) {
        // Temukan robot collector berdasarkan device ID
        Collector iotCollector = collectorRepository.findByIotDeviceId(request.getDeviceId())
                .orElseThrow(() -> new UnauthorizedException(
                        "Device IoT tidak terdaftar: " + request.getDeviceId()));

        Citizen citizen = citizenRepository.findById(request.getCitizenId())
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", request.getCitizenId()));
        WasteCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", request.getCategoryId()));

        // Buat deposit IoT dan auto-konfirmasi (sensor sudah terverifikasi)
        WasteDeposit deposit = new WasteDeposit(
                citizen, category, request.getWeightKg(),
                request.getDeviceId(), request.getLocation()
        );

        // Hitung poin menggunakan Strategy Pattern (Polymorphism)
        double points = pointCalculatorContext.calculate(request.getWeightKg(), category);

        // Auto-konfirmasi karena dari perangkat IoT terpercaya
        deposit.confirm(iotCollector, points, "IoT-AUTO-CONFIRM");

        // Kreditkan poin ke Green Wallet citizen
        GreenWallet wallet = walletRepository.findByCitizen(citizen)
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenId", citizen.getId()));
        wallet.addPoints(points);
        walletRepository.save(wallet);

        WasteDeposit saved = depositRepository.save(deposit);
        
        // Cek dan berikan badge
        achievementService.checkAndAwardBadges(citizen);
        
        log.info("IoT Deposit dari {} — citizen: {}, kategori: {}, poin: {}",
                request.getDeviceId(), citizen.getName(), category.getName(), points);

        return mapToResponse(saved);
    }

    // ==================== Confirm Deposit (Polymorphism — Strategy Pattern) ====================

    @Override
    @Transactional
    public WasteDepositResponse confirmDeposit(String depositId, String collectorEmail, String pickupProofUrl) {
        WasteDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("WasteDeposit", "id", depositId));

        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw new IllegalStateException(
                    "Hanya setoran berstatus PENDING yang bisa dikonfirmasi. Status saat ini: " + deposit.getStatus());
        }

        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));

        // POLYMORPHISM VIA STRATEGY PATTERN
        // PointCalculatorContext memilih strategy yang tepat berdasarkan WasteType:
        //   ORGANIC   → OrganicPointCalculator  (1.0x)
        //   INORGANIC → InorganicPointCalculator (1.5x)
        //   B3        → B3PointCalculator        (2.0x + bonus)
        double points = pointCalculatorContext.calculate(
                deposit.getWeightKg(), deposit.getCategory());

        log.info("Menghitung poin dengan strategy: {}",
                pointCalculatorContext.getStrategyName(deposit.getCategory().getType()));

        // Konfirmasi deposit (encapsulated dalam entity method)
        deposit.confirm(collector, points, pickupProofUrl);
        depositRepository.save(deposit);

        // Feature C: Update collector load (Null-safe check)
        double currentLoad = (collector.getCurrentLoadKg() != null) ? collector.getCurrentLoadKg() : 0.0;
        double maxCapacity = (collector.getMaxCapacityKg() != null) ? collector.getMaxCapacityKg() : 500.0;

        if (currentLoad + deposit.getWeightKg() > maxCapacity) {
            log.warn("Kapasitas kendaraan collector {} sudah penuh!", collectorEmail);
            // Tetap izinkan untuk demo, tapi di real-world mungkin diblokir
        }
        collector.setCurrentLoadKg(currentLoad + deposit.getWeightKg());
        collectorRepository.save(collector);

        // Kreditkan poin ke GreenWallet citizen (Encapsulation: logika di entity)
        GreenWallet wallet = walletRepository.findByCitizen(deposit.getCitizen())
                .orElseThrow(() -> new ResourceNotFoundException("GreenWallet", "citizenId",
                        deposit.getCitizen().getId()));
        wallet.addPoints(points);
        walletRepository.save(wallet);

        log.info("Deposit {} dikonfirmasi. Poin {} dikreditkan ke wallet {}",
                depositId, points, deposit.getCitizen().getEmail());

        notificationService.sendNotification(
            deposit.getCitizen(),
            "Setoran Dikonfirmasi!",
            String.format("Setoran %s sebesar %.1f kg telah dikonfirmasi. Anda mendapatkan +%.0f poin!", deposit.getCategory().getName(), deposit.getWeightKg(), points),
            "SUCCESS"
        );
        
        // Cek dan berikan badge
        achievementService.checkAndAwardBadges(deposit.getCitizen());

        // Broadcast points update to the citizen
        messagingTemplate.convertAndSend("/queue/citizen/" + deposit.getCitizen().getEmail() + "/wallet", "POINTS_UPDATED");
        // Broadcast deposit status update
        messagingTemplate.convertAndSend("/queue/citizen/" + deposit.getCitizen().getEmail() + "/deposits", "DEPOSIT_CONFIRMED");

        return mapToResponse(deposit);
    }

    // ==================== Reject Deposit ====================

    @Override
    @Transactional
    public WasteDepositResponse rejectDeposit(String depositId, String collectorEmail, String reason) {
        WasteDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("WasteDeposit", "id", depositId));

        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw new IllegalStateException("Hanya setoran berstatus PENDING yang bisa ditolak.");
        }

        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));

        deposit.reject(collector, reason);
        depositRepository.save(deposit);

        notificationService.sendNotification(
            deposit.getCitizen(),
            "Setoran Ditolak",
            String.format("Setoran %s sebesar %.1f kg ditolak. Alasan: %s", deposit.getCategory().getName(), deposit.getWeightKg(), reason),
            "ERROR"
        );

        return mapToResponse(deposit);
    }

    // ==================== Query Methods ====================

    @Override
    public Page<WasteDepositResponse> getMyDeposits(String citizenEmail, Pageable pageable) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        return depositRepository.findByCitizen(citizen, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WasteDepositResponse> getAllDeposits(String search, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String statusStr, Pageable pageable) {
        DepositStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = DepositStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Abaikan jika status tidak valid
            }
        }
        return depositRepository.findWithFilters(search, status, startDate, endDate, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<WasteDepositResponse> getPendingDeposits(Pageable pageable) {
        return depositRepository.findByStatus(DepositStatus.PENDING, pageable).map(this::mapToResponse);
    }

    @Override
    public WasteDepositResponse getById(String depositId) {
        WasteDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("WasteDeposit", "id", depositId));
        return mapToResponse(deposit);
    }

    @Override
    public Page<WasteDepositResponse> getCollectorHistory(String collectorEmail, Pageable pageable) {
        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));
        // Find deposits handled by this collector
        return depositRepository.findByCollector(collector, pageable).map(this::mapToResponse);
    }

    // ==================== Mapper (Encapsulation) ====================

    /** Mapper: WasteDeposit entity → WasteDepositResponse DTO */
    @Override
    public java.util.List<WasteDepositResponse> getConfirmedByCollector(String collectorId) {
        return collectorRepository.findById(collectorId)
                .map(collector -> depositRepository.findByCollectorAndStatusOrderByConfirmedAtDesc(collector, DepositStatus.CONFIRMED)
                        .stream()
                        .map(this::mapToResponse)
                        .collect(java.util.stream.Collectors.toList()))
                .orElse(java.util.List.of());
    }

    private WasteDepositResponse mapToResponse(WasteDeposit deposit) {
        String finalLocation = deposit.getLocation();
        if ((finalLocation == null || finalLocation.isEmpty()) && deposit.getCitizen() != null) {
            finalLocation = deposit.getCitizen().getAddress();
        }

        return WasteDepositResponse.builder()
                .id(deposit.getId())
                .citizenName(deposit.getCitizen() != null ? deposit.getCitizen().getName() : "Anonim")
                .citizenId(deposit.getCitizen() != null ? deposit.getCitizen().getId() : null)
                .citizenPhone(deposit.getCitizen() != null ? deposit.getCitizen().getPhone() : "-")
                .categoryName(deposit.getCategory() != null ? deposit.getCategory().getName() : "-")
                .categoryType(deposit.getCategory() != null && deposit.getCategory().getType() != null 
                        ? deposit.getCategory().getType().name() : "INORGANIC")
                .collectorName(deposit.getCollector() != null ? deposit.getCollector().getName() : null)
                .weightKg(deposit.getWeightKg())
                .pointsEarned(deposit.getPointsEarned())
                .pointsPerKg(deposit.getCategory() != null ? deposit.getCategory().getPointsPerKg() : 0)
                .status(deposit.getStatus())
                .notes(deposit.getNotes())
                .imageUrl(deposit.getImageUrl())
                .fromIoT(deposit.isFromIoT())
                .iotDeviceId(deposit.getIotDeviceId())
                .location(finalLocation != null ? finalLocation : "-")
                .confirmedAt(deposit.getConfirmedAt())
                .createdAt(deposit.getCreatedAt())
                .pickupProofUrl(deposit.getPickupProofUrl())
                .citizenDepositCount(0)
                .citizenTotalWeight(0.0)
                .build();
    }

    @Override
    public long countByCollector(com.smartwaste.entity.Collector collector) {
        return depositRepository.countByCollector(collector);
    }

    @Override
    public long countByCollectorAndStatus(com.smartwaste.entity.Collector collector, DepositStatus status) {
        return depositRepository.countByCollectorAndStatus(collector, status);
    }

    @Override
    public double getTodayWeightByCollector(String collectorEmail) {
        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime endOfDay = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        return depositRepository.sumTodayWeightByCollector(collector, startOfDay, endOfDay);
    }

    @Override
    public long countPendingDeposits() {
        return depositRepository.countByStatus(DepositStatus.PENDING);
    }

    @Override
    public java.util.List<Object[]> getCategoryStatsByCollector(String collectorEmail) {
        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));
        return depositRepository.findCategoryStatsByCollector(collector);
    }

    @Override
    @Transactional
    public void createManualDeposit(String collectorEmail, String citizenId, String categoryId, double weightKg, String notes) {
        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        WasteCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", categoryId));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Kategori sampah ini sudah tidak aktif.");
        }

        // Buat deposit langsung CONFIRMED karena petugas langsung memverifikasi
        WasteDeposit deposit = new WasteDeposit(citizen, category, weightKg, notes);
        deposit.setCollector(collector);
        deposit.setStatus(DepositStatus.CONFIRMED);
        deposit.setConfirmedAt(java.time.LocalDateTime.now());

        // Hitung & kredit poin ke wallet warga
        double points = pointCalculatorContext.calculate(weightKg, category);
        deposit.setPointsEarned(points);
        depositRepository.save(deposit);

        // Update wallet warga
        GreenWallet wallet = walletRepository.findByCitizen(citizen)
                .orElseGet(() -> {
                    GreenWallet w = new GreenWallet(citizen);
                    return walletRepository.save(w);
                });
        wallet.addPoints(points);
        walletRepository.save(wallet);

        // Feature C: Update collector load for manual deposits (consistency fix)
        double currentLoad = (collector.getCurrentLoadKg() != null) ? collector.getCurrentLoadKg() : 0.0;
        collector.setCurrentLoadKg(currentLoad + weightKg);
        collectorRepository.save(collector);
        
        // Cek dan berikan badge
        achievementService.checkAndAwardBadges(citizen);

        log.info("[MANUAL-DEPOSIT] Collector {} mencatat setoran manual untuk Citizen {} - {} kg {} = {} poin. New Load: {} kg",
                collectorEmail, citizen.getName(), weightKg, category.getName(), points, collector.getCurrentLoadKg());
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getCollectionTrendByCollector(String collectorEmail) {
        return collectorRepository.findByEmail(collectorEmail).map(collector -> {
            java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7).withHour(0).withMinute(0).withSecond(0);
            return depositRepository.findDailyTrendByCollector(collector, sevenDaysAgo).stream()
                    .map(row -> {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("date", row[0].toString());
                        map.put("weight", row[1]);
                        return map;
                    }).toList();
        }).orElse(java.util.List.of());
    }

    @Override
    public double getTotalWeightByCollector(String collectorEmail) {
        return collectorRepository.findByEmail(collectorEmail)
                .map(depositRepository::sumTotalWeightByCollector)
                .orElse(0.0);
    }

    @Override
    public double getTotalPointsByCollector(String collectorEmail) {
        return collectorRepository.findByEmail(collectorEmail)
                .map(depositRepository::sumTotalPointsByCollector)
                .orElse(0.0);
    }

    @Override
    public long countUniqueCitizensServedByCollector(String collectorEmail) {
        return collectorRepository.findByEmail(collectorEmail)
                .map(depositRepository::countUniqueCitizensByCollector)
                .orElse(0L);
    }

    @Override
    @Transactional
    public void deleteDeposit(String depositId) {
        WasteDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("WasteDeposit", "id", depositId));
        
        // Jika sudah confirmed, kurangi poin dari wallet warga (opsional, tapi disarankan untuk integritas)
        if (deposit.getStatus() == DepositStatus.CONFIRMED && deposit.getCitizen() != null) {
            walletRepository.findByCitizen(deposit.getCitizen()).ifPresent(wallet -> {
                wallet.setTotalPoints(wallet.getTotalPoints() - deposit.getPointsEarned());
                walletRepository.save(wallet);
            });
        }
        
        depositRepository.delete(deposit);
    }
}
