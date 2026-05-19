package com.smartwaste.controller;

import com.smartwaste.dto.request.CreateWasteDepositRequest;
import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.WasteDepositResponse;
import com.smartwaste.service.WasteDepositService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Controller untuk manajemen setoran sampah.
 * Mendukung CRUD + konfirmasi/penolakan oleh Collector.
 */
@RestController
@RequestMapping("/api/v1/deposits")
@Tag(name = "Waste Deposit", description = "Transaksi setor sampah")
@SecurityRequirement(name = "bearerAuth")
public class WasteDepositController {

    private final WasteDepositService depositService;

    public WasteDepositController(WasteDepositService depositService) {
        this.depositService = depositService;
    }

    /** Citizen membuat setoran sampah baru */
    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Buat setoran baru", description = "Citizen menyetorkan sampah — status PENDING")
    public ResponseEntity<ApiResponse<WasteDepositResponse>> createDeposit(
            @Valid @RequestBody CreateWasteDepositRequest request,
            Authentication auth) {
        WasteDepositResponse response = depositService.createDeposit(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Setoran berhasil dibuat. Menunggu konfirmasi petugas.", response));
    }

    /** Riwayat setoran citizen yang sedang login */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Riwayat setoran saya")
    public ResponseEntity<ApiResponse<Page<WasteDepositResponse>>> getMyDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Page<WasteDepositResponse> deposits = depositService.getMyDeposits(
                auth.getName(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Data setoran berhasil diambil.", deposits));
    }

    /** Semua setoran (Admin & Collector) */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Semua setoran (Admin/Collector)")
    public ResponseEntity<ApiResponse<Page<WasteDepositResponse>>> getAllDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WasteDepositResponse> deposits = depositService.getAllDeposits(
                null, null, null, null, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Data berhasil diambil.", deposits));
    }

    /** Setoran yang PENDING (untuk collector) */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'COLLECTOR')")
    @Operation(summary = "Setoran menunggu konfirmasi")
    public ResponseEntity<ApiResponse<Page<WasteDepositResponse>>> getPendingDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Data berhasil diambil.",
                depositService.getPendingDeposits(PageRequest.of(page, size))));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('COLLECTOR', 'ADMIN')")
    @Operation(summary = "Konfirmasi setoran", description = "Poin otomatis dihitung dan dikreditkan ke Green Wallet warga")
    public ResponseEntity<ApiResponse<WasteDepositResponse>> confirmDeposit(
            @PathVariable String id, 
            @RequestParam(required = false) String pickupProofUrl,
            Authentication auth) {
        WasteDepositResponse response = depositService.confirmDeposit(id, auth.getName(), pickupProofUrl != null ? pickupProofUrl : "");
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Setoran dikonfirmasi! %.0f poin dikreditkan ke Green Wallet.", response.getPointsEarned()),
                response));
    }

    /** Collector menolak setoran */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('COLLECTOR', 'ADMIN')")
    @Operation(summary = "Tolak setoran")
    public ResponseEntity<ApiResponse<WasteDepositResponse>> rejectDeposit(
            @PathVariable String id,
            @RequestParam(defaultValue = "Tidak sesuai standar") String reason,
            Authentication auth) {
        WasteDepositResponse response = depositService.rejectDeposit(id, auth.getName(), reason);
        return ResponseEntity.ok(ApiResponse.success("Setoran ditolak.", response));
    }

    /** Detail satu setoran */
    @GetMapping("/{id}")
    @Operation(summary = "Detail setoran")
    public ResponseEntity<ApiResponse<WasteDepositResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Data berhasil diambil.", depositService.getById(id)));
    }
}
