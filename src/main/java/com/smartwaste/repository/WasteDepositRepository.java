package com.smartwaste.repository;

import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.WasteDeposit;
import com.smartwaste.entity.enums.DepositStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository untuk entitas {@link WasteDeposit}.
 * Berisi query JPQL kustom untuk laporan agregasi.
 */
@Repository
public interface WasteDepositRepository extends JpaRepository<WasteDeposit, String> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen", "category"})
    Page<WasteDeposit> findByCitizen(Citizen citizen, Pageable pageable);

    Page<WasteDeposit> findByCollector(com.smartwaste.entity.Collector collector, Pageable pageable);

    Page<WasteDeposit> findByCitizenAndStatus(Citizen citizen, DepositStatus status, Pageable pageable);

    long countByCitizenAndStatus(Citizen citizen, DepositStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen", "category"})
    Page<WasteDeposit> findByStatus(DepositStatus status, Pageable pageable);

    long countByStatus(DepositStatus status);

    /** Total berat semua sampah yang sudah dikonfirmasi */
    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM WasteDeposit d WHERE d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED")
    double sumTotalWeightConfirmed();

    /** Total poin yang sudah didistribusikan */
    @Query("SELECT COALESCE(SUM(d.pointsEarned), 0) FROM WasteDeposit d WHERE d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED")
    double sumTotalPointsDistributed();

    /** Total berat per citizen */
    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM WasteDeposit d WHERE d.citizen = :citizen AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED")
    double sumWeightByCitizen(@Param("citizen") Citizen citizen);

    /** Statistik bulanan untuk grafik admin */
    @Query("SELECT FUNCTION('DATE_FORMAT', d.createdAt, '%Y-%m') AS monthYear, " +
           "COUNT(d) AS depositCount, SUM(d.weightKg) AS totalWeight, SUM(d.pointsEarned) AS totalPoints " +
           "FROM WasteDeposit d WHERE d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED " +
           "GROUP BY FUNCTION('DATE_FORMAT', d.createdAt, '%Y-%m') " +
           "ORDER BY monthYear DESC")
    List<Object[]> findMonthlyStats();

    /** Statistik per kategori */
    @Query("SELECT d.category.name, SUM(d.weightKg), COUNT(d) " +
           "FROM WasteDeposit d WHERE d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED " +
           "GROUP BY d.category.name")
    List<Object[]> findCategoryStats();

    /** Setoran dari perangkat IoT */
    Page<WasteDeposit> findByFromIoTTrue(Pageable pageable);

    /** Setoran berdasarkan IoT device ID */
    Page<WasteDeposit> findByIotDeviceId(String iotDeviceId, Pageable pageable);

    /** Total setoran yang dikonfirmasi oleh satu collector */
    long countByCollector(com.smartwaste.entity.Collector collector);

    /** Total setoran per status per collector */
    long countByCollectorAndStatus(com.smartwaste.entity.Collector collector, DepositStatus status);

    /** Total berat sampah yang dikonfirmasi oleh collector hari ini */
    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM WasteDeposit d WHERE d.collector = :collector AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED AND d.confirmedAt >= :startOfDay AND d.confirmedAt <= :endOfDay")
    double sumTodayWeightByCollector(@Param("collector") com.smartwaste.entity.Collector collector, @Param("startOfDay") java.time.LocalDateTime startOfDay, @Param("endOfDay") java.time.LocalDateTime endOfDay);

    /** Statistik per kategori untuk collector tertentu */
    @Query("SELECT d.category.name, SUM(d.weightKg), COUNT(d) " +
           "FROM WasteDeposit d WHERE d.collector = :collector AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED " +
           "GROUP BY d.category.name")
    List<Object[]> findCategoryStatsByCollector(@Param("collector") com.smartwaste.entity.Collector collector);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen", "category"})
    Page<WasteDeposit> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen", "category"})
    Page<WasteDeposit> findByStatusAndCreatedAtBetween(DepositStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen", "category"})
    @Query("SELECT d FROM WasteDeposit d WHERE (:search IS NULL OR :search = '' OR LOWER(d.citizen.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (cast(:startDate as timestamp) IS NULL OR d.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR d.createdAt <= :endDate)")
    Page<WasteDeposit> findWithFilters(@Param("search") String search, 
                                       @Param("status") DepositStatus status, 
                                       @Param("startDate") java.time.LocalDateTime startDate, 
                                       @Param("endDate") java.time.LocalDateTime endDate, 
                                       Pageable pageable);

    /** Statistik per kategori untuk citizen tertentu */
    @Query("SELECT d.category.name, SUM(d.weightKg), COUNT(d) " +
           "FROM WasteDeposit d WHERE d.citizen = :citizen AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED " +
           "GROUP BY d.category.name")
    List<Object[]> findCategoryStatsByCitizen(@Param("citizen") Citizen citizen);

    /** Statistik bulanan untuk citizen tertentu */
    @Query("SELECT FUNCTION('DATE_FORMAT', d.createdAt, '%Y-%m') AS monthYear, " +
           "COUNT(d) AS depositCount, SUM(d.weightKg) AS totalWeight, SUM(d.pointsEarned) AS totalPoints " +
           "FROM WasteDeposit d WHERE d.citizen = :citizen AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED " +
           "GROUP BY FUNCTION('DATE_FORMAT', d.createdAt, '%Y-%m') " +
           "ORDER BY monthYear ASC")
    List<Object[]> findMonthlyStatsByCitizen(@Param("citizen") Citizen citizen);

    /** Analytics: Tren harian collector (7 hari terakhir) */
    @Query("SELECT FUNCTION('DATE', d.confirmedAt) AS collectDate, SUM(d.weightKg) " +
           "FROM WasteDeposit d WHERE d.collector = :collector AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED " +
           "AND d.confirmedAt >= :sevenDaysAgo " +
           "GROUP BY FUNCTION('DATE', d.confirmedAt) " +
           "ORDER BY collectDate ASC")
    List<Object[]> findDailyTrendByCollector(@Param("collector") com.smartwaste.entity.Collector collector, @Param("sevenDaysAgo") java.time.LocalDateTime sevenDaysAgo);

    /** Analytics: Total berat kumulatif collector */
    @Query("SELECT COALESCE(SUM(d.weightKg), 0) FROM WasteDeposit d WHERE d.collector = :collector AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED")
    double sumTotalWeightByCollector(@Param("collector") com.smartwaste.entity.Collector collector);

    /** Analytics: Total poin disalurkan oleh collector */
    @Query("SELECT COALESCE(SUM(d.pointsEarned), 0) FROM WasteDeposit d WHERE d.collector = :collector AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED")
    double sumTotalPointsByCollector(@Param("collector") com.smartwaste.entity.Collector collector);

    /** Analytics: Jumlah warga unik dilayani oleh collector */
    @Query("SELECT COUNT(DISTINCT d.citizen) FROM WasteDeposit d WHERE d.collector = :collector AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED")
    long countUniqueCitizensByCollector(@Param("collector") com.smartwaste.entity.Collector collector);

    List<WasteDeposit> findByCollectorAndStatusOrderByConfirmedAtDesc(com.smartwaste.entity.Collector collector, DepositStatus status);

    @Query("SELECT COUNT(DISTINCT d.citizen) FROM WasteDeposit d WHERE d.createdAt >= :since")
    long countDistinctCitizensSince(@Param("since") java.time.LocalDateTime since);
}
