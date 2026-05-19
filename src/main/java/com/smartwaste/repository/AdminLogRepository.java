package com.smartwaste.repository;

import com.smartwaste.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, String> {
    List<AdminLog> findAllByOrderByCreatedAtDesc();
}
