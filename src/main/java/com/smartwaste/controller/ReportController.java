package com.smartwaste.controller;

import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.ReportSummaryResponse;
import com.smartwaste.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Report API", description = "Export data ke PDF & CSV")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Ringkasan laporan keseluruhan sistem")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Laporan berhasil diambil.", reportService.getSummary()));
    }
}
