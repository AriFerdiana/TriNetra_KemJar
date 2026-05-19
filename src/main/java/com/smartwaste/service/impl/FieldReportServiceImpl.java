package com.smartwaste.service.impl;

import com.smartwaste.entity.FieldReport;
import com.smartwaste.repository.FieldReportRepository;
import com.smartwaste.service.FieldReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FieldReportServiceImpl implements FieldReportService {

    private final FieldReportRepository repository;

    public FieldReportServiceImpl(FieldReportRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FieldReport> getAllReports(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldReport> getReportById(String id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public void resolveReport(String id, String resolutionDetails) {
        repository.findById(id).ifPresent(report -> {
            report.setStatus("RESOLVED");
            // If we had a resolution field in entity, we would set it here
            // For now, status change is enough
            repository.save(report);
        });
    }

    @Override
    @Transactional
    public void rejectReport(String id, String reason) {
        repository.findById(id).ifPresent(report -> {
            report.setStatus("REJECTED");
            repository.save(report);
        });
    }
}
