package com.smartwaste.repository;

import com.smartwaste.entity.Citizen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository untuk entitas {@link Citizen}.
 * Berisi query JPQL kustom untuk laporan dan manajemen data warga.
 */
@Repository
public interface CitizenRepository extends JpaRepository<Citizen, String> {

    Optional<Citizen> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNik(String nik);

    /** Cari semua warga aktif dengan pagination. */
    Page<Citizen> findByActiveTrue(Pageable pageable);

    /** Full-text search warga berdasarkan nama, email, atau NIK dengan filter status. */
    @Query("SELECT c FROM Citizen c WHERE " +
           "(:active IS NULL OR c.active = :active) AND " +
           "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " c.nik LIKE CONCAT('%', :keyword, '%'))")
    Page<Citizen> searchCitizens(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);

    /** Total jumlah warga aktif (untuk dashboard admin). */
    long countByActiveTrue();

    /** Top N citizen berdasarkan total poin wallet. */
    @Query("SELECT c FROM Citizen c JOIN c.wallet w ORDER BY w.totalPoints DESC")
    Page<Citizen> findTopByPoints(Pageable pageable);
}
