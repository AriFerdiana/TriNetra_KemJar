package com.smartwaste.service;

import com.smartwaste.entity.FieldReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface FieldReportService {
    Page<FieldReport> getAllReports(Pageable pageable);
    Optional<FieldReport> getReportById(String id);
    void resolveReport(String id, String resolutionDetails);
    void rejectReport(String id, String reason);
}
