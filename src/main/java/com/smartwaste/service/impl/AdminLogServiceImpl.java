package com.smartwaste.service.impl;

import com.smartwaste.entity.AdminLog;
import com.smartwaste.repository.AdminLogRepository;
import com.smartwaste.service.AdminLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminLogServiceImpl implements AdminLogService {

    private final AdminLogRepository logRepository;

    public AdminLogServiceImpl(AdminLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public void log(String action, String details, String ipAddress) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication() != null ? 
                SecurityContextHolder.getContext().getAuthentication().getName() : "SYSTEM";
        
        AdminLog log = AdminLog.builder()
                .adminEmail(adminEmail)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        
        logRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminLog> getAllLogs() {
        return logRepository.findAllByOrderByCreatedAtDesc();
    }
}
