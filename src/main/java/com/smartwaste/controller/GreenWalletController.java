package com.smartwaste.controller;

import com.smartwaste.dto.request.RedeemPointsRequest;
import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.WalletResponse;
import com.smartwaste.service.GreenWalletService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "Green Wallet", description = "Manajemen poin gamifikasi warga")
@SecurityRequirement(name = "bearerAuth")
public class GreenWalletController {

    private final GreenWalletService walletService;

    public GreenWalletController(GreenWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Lihat saldo wallet saya")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Data wallet berhasil diambil.",
                walletService.getMyWallet(auth.getName())));
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Tukar poin dengan reward (Request)")
    public ResponseEntity<ApiResponse<com.smartwaste.entity.PointRedemption>> redeemPoints(
            @Valid @RequestBody RedeemPointsRequest request,
            Authentication auth) {
        com.smartwaste.entity.PointRedemption response = walletService.requestRedemption(
                auth.getName(), request.getPoints(), request.getDescription(), request.getRewardItemId());
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Penukaran %.0f poin berhasil diajukan dan berstatus PENDING!", request.getPoints()), response));
    }

    @GetMapping("/{citizenId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lihat wallet citizen (Admin)")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByCitizenId(@PathVariable String citizenId) {
        return ResponseEntity.ok(ApiResponse.success("Data wallet berhasil diambil.",
                walletService.getWalletByCitizenId(citizenId)));
    }
}
