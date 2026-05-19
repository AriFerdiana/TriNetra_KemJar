package com.smartwaste.repository;

import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByCitizenAndIsReadFalseOrderByCreatedAtDesc(Citizen citizen);
    Page<Notification> findByCitizenOrderByCreatedAtDesc(Citizen citizen, Pageable pageable);
}
