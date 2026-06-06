package com.smartwaste.repository;

import com.smartwaste.entity.Collector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository untuk entitas {@link Collector}.
 */
@Repository
public interface CollectorRepository extends JpaRepository<Collector, String> {

    Optional<Collector> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Collector> findByAvailableTrue();

    Optional<Collector> findByIotDeviceId(String iotDeviceId);

    boolean existsByIotDeviceId(String iotDeviceId);

    long countByActiveTrue();

    Page<Collector> findByActiveTrue(Pageable pageable);

    /**
     * Leaderboard collector berdasarkan jumlah setoran yang dikonfirmasi.
     * Menggunakan correlated subquery agar enum-safe dan kompatibel dengan MySQL strict mode.
     * Hanya menampilkan collector aktif (bukan perangkat IoT).
     */
    @Query("SELECT c.name, (SELECT COALESCE(SUM(d.weightKg), 0) FROM WasteDeposit d " +
           "                WHERE d.collector = c " +
           "                AND d.status = com.smartwaste.entity.enums.DepositStatus.CONFIRMED) AS totalWeight " +
           "FROM Collector c " +
           "WHERE c.active = true AND c.iotDevice = false " +
           "ORDER BY totalWeight DESC")
    List<Object[]> getCollectorLeaderboard();

    @Query("SELECT c FROM Collector c WHERE c.iotDevice = false AND " +
           "(:active IS NULL OR c.active = :active) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Collector> searchCollectors(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
