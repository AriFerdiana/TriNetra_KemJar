package com.smartwaste.service;

import com.smartwaste.entity.AdminLog;
import java.util.List;

public interface AdminLogService {
    void log(String action, String details, String ipAddress);
    List<AdminLog> getAllLogs();
}
