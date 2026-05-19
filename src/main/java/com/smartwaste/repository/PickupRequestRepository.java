package com.smartwaste.repository;

import com.smartwaste.entity.PickupRequest;
import com.smartwaste.entity.Citizen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PickupRequestRepository extends JpaRepository<PickupRequest, String> {
    Page<PickupRequest> findByCitizenOrderByPickupDateDesc(Citizen citizen, Pageable pageable);
    
    java.util.Optional<PickupRequest> findTopByCitizenOrderByCreatedAtDesc(Citizen citizen);
    
    long countByCitizenAndStatus(Citizen citizen, com.smartwaste.entity.enums.PickupStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT p.pickupDate FROM PickupRequest p WHERE p.citizen = :citizen AND p.pickupDate >= :start AND p.pickupDate <= :end")
    java.util.List<java.time.LocalDate> findPickupDatesByCitizenAndDateRange(@org.springframework.data.repository.query.Param("citizen") Citizen citizen, 
                                                                           @org.springframework.data.repository.query.Param("start") java.time.LocalDate start, 
                                                                           @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);
}
