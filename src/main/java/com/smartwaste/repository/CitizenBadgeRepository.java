package com.smartwaste.repository;

import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.CitizenBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenBadgeRepository extends JpaRepository<CitizenBadge, String> {
    List<CitizenBadge> findByCitizen(Citizen citizen);
    boolean existsByCitizenAndBadge_Id(Citizen citizen, String badgeId);
}
