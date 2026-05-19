package com.smartwaste.repository;

import com.smartwaste.entity.FieldReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldReportRepository extends JpaRepository<FieldReport, String> {
}
