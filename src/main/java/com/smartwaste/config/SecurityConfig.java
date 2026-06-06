package com.smartwaste.config;

import com.smartwaste.service.AdminLogService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Konfigurasi Spring Security utama.
 *
 * <p>Menerapkan <b>Role-Based Access Control (RBAC)</b> secara deklaratif:
 * setiap endpoint dikunci hanya untuk role tertentu.</p>
 *
 * <p>Strategi keamanan berlapis:</p>
 * <ul>
 *   <li><b>Rate Limiting</b>: {@link RateLimitingFilter} mencegah Brute Force.</li>
 *   <li><b>Security Headers</b>: CSP, X-Frame-Options, X-XSS-Protection.</li>
 *   <li><b>Web pages</b>: Form login + Session (untuk Thymeleaf).</li>
 *   <li><b>REST API (/api/**)</b>: Stateless JWT via Bearer token.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;
    private final AdminLogService adminLogService;
    private final MfaVerificationFilter mfaVerificationFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RateLimitingFilter rateLimitingFilter,
                          UserDetailsService userDetailsService,
                          AdminLogService adminLogService,
                          MfaVerificationFilter mfaVerificationFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.userDetailsService = userDetailsService;
        this.adminLogService = adminLogService;
        this.mfaVerificationFilter = mfaVerificationFilter;
    }

    // ==================== Filter Chain 1: REST API (Stateless JWT) ====================

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Security Headers untuk API
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(ct -> {})
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            )
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — terbuka untuk semua
                .requestMatchers("/api/v1/auth/**").permitAll()
                // IoT endpoint — cukup dengan API key (divalidasi di controller)
                .requestMatchers("/api/v1/iot/**").permitAll()
                // Chatbot anonim
                .requestMatchers("/api/v1/chat/anonymous").permitAll()
                // Swagger / OpenAPI docs — terbuka
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Admin only
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
                // Collector only
                .requestMatchers("/api/v1/deposits/*/confirm").hasAnyRole("COLLECTOR", "ADMIN")
                .requestMatchers("/api/v1/deposits/*/reject").hasAnyRole("COLLECTOR", "ADMIN")
                // Citizen endpoints
                .requestMatchers("/api/v1/wallet/**").hasRole("CITIZEN")
                .requestMatchers("/api/v1/chat/**").hasAnyRole("CITIZEN", "ADMIN")
                // Report — Admin & Collector can view
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "COLLECTOR")
                // Semua API lainnya harus terautentikasi
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ==================== Filter Chain 2: Web Pages (Session + Form Login) ====================

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/internal/**"))
            // ===== SECURITY HEADERS (Proteksi XSS, Clickjacking, MIME Sniffing) =====
            .headers(headers -> headers
                // Mencegah Clickjacking — halaman tidak bisa di-load dalam iframe
                .frameOptions(frame -> frame.deny())
                // Mencegah MIME type sniffing — browser harus ikut Content-Type yang dikirim server
                .contentTypeOptions(ct -> {})
                // X-XSS-Protection: Browser langsung blokir halaman jika deteksi XSS
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                // Content-Security-Policy — Sumber yang diizinkan untuk script, style, gambar
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                        "https://cdn.tailwindcss.com " +
                        "https://cdn.jsdelivr.net " +
                        "https://cdnjs.cloudflare.com " +
                        "https://code.jquery.com " +
                        "https://unpkg.com " +
                        "https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/ " +
                        "https://cdn.jsdelivr.net/npm/sweetalert2@11; " +
                    "style-src 'self' 'unsafe-inline' " +
                        "https://cdn.tailwindcss.com " +
                        "https://cdn.jsdelivr.net " +
                        "https://cdnjs.cloudflare.com " +
                        "https://fonts.googleapis.com " +
                        "https://unpkg.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; " +
                    "img-src 'self' data: blob: https:; " +
                    "connect-src 'self' https:; " +
                    "frame-ancestors 'none'; " +
                    "form-action 'self'"
                ))
                // Referrer Policy — Jangan bocorkan URL referrer ke website lain
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeHttpRequests(auth -> auth
                // Halaman publik
                .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**",
                                 "/webjars/**", "/favicon.ico", "/uploads/**", "/error").permitAll()
                // Endpoint MFA — harus terautentikasi
                .requestMatchers("/mfa/**").authenticated()
                // Halaman admin
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Halaman citizen
                .requestMatchers("/citizen/**").hasRole("CITIZEN")
                // Halaman collector
                .requestMatchers("/collector/**").hasRole("COLLECTOR")
                // API Internal (Session-based)
                .requestMatchers("/internal/**").authenticated()
                // Semua halaman lain harus login
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    // Log aksi login untuk ADMIN
                    var authorities = authentication.getAuthorities();
                    boolean isAdmin = authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    if (isAdmin) {
                        String ip = request.getRemoteAddr();
                        adminLogService.log("LOGIN_ADMIN",
                            "Admin login: " + authentication.getName(), ip);
                    }

                    // Cek apakah ada URL yang disimpan (misal: user mengetik /mfa/setup)
                    org.springframework.security.web.savedrequest.RequestCache requestCache = 
                        new org.springframework.security.web.savedrequest.HttpSessionRequestCache();
                    org.springframework.security.web.savedrequest.SavedRequest savedRequest = 
                        requestCache.getRequest(request, response);

                    String redirectUrl = null;

                    if (savedRequest != null && savedRequest.getMethod().equalsIgnoreCase("GET")) {
                        redirectUrl = savedRequest.getRedirectUrl();
                    } else {
                        // Default redirect berdasarkan role
                        for (var authority : authorities) {
                            redirectUrl = switch (authority.getAuthority()) {
                                case "ROLE_ADMIN"     -> "/admin/dashboard";
                                case "ROLE_CITIZEN"   -> "/citizen/dashboard";
                                case "ROLE_COLLECTOR" -> "/collector/dashboard";
                                default -> "/";
                            };
                            break;
                        }
                    }
                    response.sendRedirect(redirectUrl);
                })
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID") // Hapus session cookie saat logout
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (accessDeniedException instanceof org.springframework.security.web.csrf.MissingCsrfTokenException
                            || accessDeniedException instanceof org.springframework.security.web.csrf.InvalidCsrfTokenException) {
                        response.sendRedirect(request.getContextPath() + "/auth/login?timeout=true");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/auth/access-denied");
                    }
                })
            )
            // RateLimitingFilter juga aktif di halaman web (proteksi form login)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            // Filter untuk enforce verifikasi MFA setelah login
            .addFilterAfter(mfaVerificationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ==================== Beans ====================

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

