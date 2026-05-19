package com.smartwaste.controller;

import com.smartwaste.dto.request.LoginRequest;
import com.smartwaste.dto.request.RegisterCitizenRequest;
import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.AuthResponse;
import com.smartwaste.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller REST API untuk autentikasi (register & login).
 *
 * <p>Endpoint ini publik — tidak memerlukan JWT token.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication API", description = "Endpoints untuk registrasi dan login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Mendaftarkan Citizen baru beserta GreenWallet-nya.
     *
     * <p>Method: POST /api/v1/auth/register</p>
     */
    @PostMapping("/register")
    @Operation(summary = "Registrasi warga baru", description = "Mendaftarkan citizen baru; GreenWallet dibuat otomatis")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterCitizenRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registrasi berhasil! Selamat datang di Smart Community Waste System.", response));
    }

    /**
     * Login untuk semua role (Admin, Citizen, Collector).
     *
     * <p>Method: POST /api/v1/auth/login</p>
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login dengan email dan password, mendapatkan JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login berhasil.", response));
    }
}
