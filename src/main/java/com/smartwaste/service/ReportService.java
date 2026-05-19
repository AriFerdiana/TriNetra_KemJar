package com.smartwaste.service;

import com.smartwaste.dto.response.ReportSummaryResponse;

/**
 * Service interface untuk laporan dan statistik sistem.
 */
public interface ReportService {
    ReportSummaryResponse getSummary();
    java.util.Map<String, Object> getCitizenEcoStats(String citizenEmail);
}
