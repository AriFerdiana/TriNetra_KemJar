package com.smartwaste.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Konfigurasi Springdoc OpenAPI (Swagger UI).
 *
 * <p>Menghasilkan dokumentasi API interaktif yang bisa diakses di
 * {@code http://localhost:8080/swagger-ui.html}. Mendukung autentikasi JWT
 * langsung dari Swagger UI (klik tombol "Authorize" dan masukkan token).</p>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI smartWasteOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TriNetra Smart-Waste-System Production-Grade API")
                .description("""
                    # 🚀 Backend REST & WebSocket API Documentation
                    Dokumentasi API Terpusat untuk **TriNetra Smart Waste System** — Platform Pengelolaan Sampah Komunitas Cerdas, IoT, & Gamifikasi.
                    
                    ---
                    
                    ### 💎 Core Architecture & Security:
                    1. **🔐 Dual-FilterChain Security (RBAC):**
                       - `/api/**` -> Stateless JWT-based authentication via `JwtAuthFilter`.
                       - `/admin/**`, `/citizen/**`, `/collector/**` -> Stateful Session-based with dynamic redirect filters.
                    2. **🛡️ Optimistic Locking Concurrency Control:**
                       - Entitas `GreenWallet` dilindungi dengan `@Version` JPA Optimistic Locking untuk mengeliminasi risiko *race condition* saat penukaran poin simultan.
                    3. **⚡ True Real-Time Event-Driven (STOMP over WebSocket):**
                       - Gateway: `/ws`
                       - Broadcast setoran baru: `/topic/deposits`
                       - Sinyal update dompet personal warga: `/queue/citizen/{email}/wallet`
                    4. **🤖 Automated IoT Smart Bin Integration:**
                       - Endpoint `/api/v1/iot/dump` untuk robot NetraDUMP otomatisasi timbang berat & klaim poin.
                    
                    ---
                    
                    ### 🔑 Cara Penggunaan Interaktif:
                    1. Jalankan autentikasi pada endpoint **`POST /api/v1/auth/login`**.
                    2. Salin token JWT dari response JSON (`token` field).
                    3. Klik tombol **`Authorize`** (ikon gembok hijau) di pojok kanan atas halaman ini.
                    4. Masukkan token dengan format Bearer, lalu klik **Authorize**.
                    """)
                .version("1.0.0-PROD")
                .contact(new Contact()
                    .name("TriNetra Team (Ari Ferdiana, Malendra Sahla Rizky, Muhamad Lingga Darmawan)")
                    .email("ariferdiana@smartwaste.com")
                    .url("https://github.com/AriFerdiana/TriNetra_Smart-Waste-System"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT"))
            )
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development (Spring Boot Engine)"),
                new Server().url("https://trinetra.smartwaste.com").description("Production API Gateway (Cloudflare Tunnel)")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Masukkan token JWT valid Anda untuk mengakses REST API terproteksi.")
                )
            );
    }
}
