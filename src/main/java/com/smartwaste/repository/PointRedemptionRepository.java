package com.smartwaste.repository;

import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.PointRedemption;
import com.smartwaste.entity.enums.RedemptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository untuk entitas {@link PointRedemption}.
 */
@Repository
public interface PointRedemptionRepository extends JpaRepository<PointRedemption, String> {

    Page<PointRedemption> findByCitizen(Citizen citizen, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen"})
    Page<PointRedemption> findByStatus(com.smartwaste.entity.enums.RedemptionStatus status, org.springframework.data.domain.Pageable pageable);

    @org.springframework.lang.NonNull
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen"})
    Page<PointRedemption> findAll(@org.springframework.lang.NonNull org.springframework.data.domain.Pageable pageable);

    long countByStatus(RedemptionStatus status);

    long countByCitizenAndStatus(Citizen citizen, RedemptionStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"citizen"})
    @org.springframework.data.jpa.repository.Query("SELECT r FROM PointRedemption r WHERE (:search IS NULL OR :search = '' OR LOWER(r.citizen.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PointRedemption> findWithSearch(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
