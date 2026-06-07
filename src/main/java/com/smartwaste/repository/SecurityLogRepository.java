package com.smartwaste.repository;

import com.smartwaste.entity.SecurityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, String> {
    
    // Mengambil X log terbaru diurutkan berdasarkan waktu (createdAt descending)
    List<SecurityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
