package com.smartwaste.controller;

import com.smartwaste.dto.request.IoTDepositRequest;
import com.smartwaste.dto.response.ApiResponse;
import com.smartwaste.dto.response.WasteDepositResponse;
import com.smartwaste.exception.UnauthorizedException;
import com.smartwaste.service.WasteDepositService;
import com.smartwaste.service.SmartBinService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller khusus untuk endpoint IoT / Robot NetraDUMP.
 *
 * <p>Endpoint ini dirancang untuk diakses oleh perangkat keras seperti:
 * <ul>
 *   <li>Smart bin dengan sensor timbangan otomatis</li>
 *   <li>Robot pengumpul sampah otonom (NetraDUMP)</li>
 *   <li>Sistem conveyor pemilah sampah berbasis AI</li>
 * </ul>
 *
 * <p><b>Autentikasi:</b> Tidak menggunakan JWT, melainkan API Key statis
 * yang dikirim via header {@code X-IoT-Api-Key}. Ini memudahkan integrasi
 * dengan firmware mikrokontroler yang tidak mendukung JWT.</p>
 *
 * <p><b>Auto-Konfirmasi:</b> Setoran dari IoT langsung CONFIRMED (tidak PENDING)
 * karena data sensor dianggap sudah terverifikasi hardware.</p>
 */
@RestController
@RequestMapping("/api/v1/iot")
@Tag(name = "IoT Sensor", description = "Webhook untuk robot NetraDUMP")
public class IoTController {

    private final WasteDepositService depositService;
    private final SmartBinService smartBinService;

    public IoTController(WasteDepositService depositService, SmartBinService smartBinService) {
        this.depositService = depositService;
        this.smartBinService = smartBinService;
    }

    @Value("${app.iot.api-key}")
    private String validIoTApiKey;

    /**
     * Endpoint utama IoT — menerima data setoran dari robot/smart bin.
     *
     * <p>Method: POST /api/v1/iot/dump</p>
     * <p>Header wajib: {@code X-IoT-Api-Key: IOT-TRINETRA-NETRADUMP-2024-SECRET}</p>
     *
     * <p>Urutan proses:</p>
     * <ol>
     *   <li>Validasi X-IoT-Api-Key</li>
     *   <li>Validasi device ID terdaftar di tabel collectors</li>
     *   <li>Hitung poin via PointCalculatorContext (Strategy Pattern)</li>
     *   <li>Auto-konfirmasi deposit</li>
     *   <li>Kredit poin ke GreenWallet citizen</li>
     * </ol>
     */
    @PostMapping("/dump")
    @Operation(
        summary = "Kirim data setoran dari perangkat IoT",
        description = "Endpoint untuk robot/smart bin NetraDUMP. Gunakan X-IoT-Api-Key di header untuk autentikasi."
    )
    public ResponseEntity<ApiResponse<WasteDepositResponse>> receiveIoTDump(
            @RequestHeader("X-IoT-Api-Key") String apiKey,
            @Valid @RequestBody IoTDepositRequest request) {

        // Validasi API key IoT (bukan JWT)
        if (!validIoTApiKey.equals(apiKey)) {
            throw new UnauthorizedException("X-IoT-Api-Key tidak valid. Akses ditolak untuk perangkat IoT.");
        }

        WasteDepositResponse response = depositService.createIoTDeposit(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        String.format("Data IoT diterima. Setoran dikonfirmasi otomatis. Poin dikreditkan: %.0f",
                                response.getPointsEarned()),
                        response));
    }

    /**
     * Health check endpoint untuk perangkat IoT.
     * Digunakan untuk memverifikasi konektivitas sebelum mengirim data.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check IoT endpoint")
    public ResponseEntity<ApiResponse<String>> healthCheck(
            @RequestHeader("X-IoT-Api-Key") String apiKey) {
        if (!validIoTApiKey.equals(apiKey)) {
            throw new UnauthorizedException("API key tidak valid.");
        }
        return ResponseEntity.ok(ApiResponse.success("IoT endpoint aktif dan siap menerima data."));
    }

    @PostMapping("/bin-status")
    @Operation(summary = "Update status dan kapasitas Smart Bin")
    public ResponseEntity<ApiResponse<String>> updateBinStatus(
            @RequestHeader("X-IoT-Api-Key") String apiKey,
            @RequestParam String deviceId,
            @RequestParam Integer fillLevel,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        
        if (!validIoTApiKey.equals(apiKey)) {
            throw new UnauthorizedException("API key tidak valid.");
        }

        smartBinService.updateCapacity(deviceId, fillLevel.doubleValue());
        
        return ResponseEntity.ok(ApiResponse.success("Status SmartBin " + deviceId + " berhasil diperbarui ke " + fillLevel + "%"));
    }
}
