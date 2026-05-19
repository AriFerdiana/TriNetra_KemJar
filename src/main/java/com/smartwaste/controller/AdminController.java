package com.smartwaste.controller;

import com.smartwaste.dto.request.RegisterCollectorRequest;
import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.CollectorResponse;
import com.smartwaste.service.CollectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin API", description = "Endpoints untuk fitur manajemen Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final CollectorService collectorService;

    public AdminController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @PostMapping("/collectors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Daftarkan collector baru")
    public ResponseEntity<ApiResponse<CollectorResponse>> registerCollector(
            @Valid @RequestBody RegisterCollectorRequest request) {
        CollectorResponse response = collectorService.registerCollector(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Collector berhasil didaftarkan.", response));
    }

    @GetMapping("/collectors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lihat semua collector")
    public ResponseEntity<ApiResponse<Page<CollectorResponse>>> getAllCollectors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Data berhasil diambil.",
                collectorService.getAllCollectors(PageRequest.of(page, size))));
    }

    @PatchMapping("/collectors/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aktifkan/nonaktifkan collector")
    public ResponseEntity<ApiResponse<Void>> toggleCollector(@PathVariable String id) {
        collectorService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("Status collector berhasil diubah."));
    }
}
