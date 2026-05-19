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

/**
 * Konfigurasi Spring Security utama.
 *
 * <p>Menerapkan <b>Role-Based Access Control (RBAC)</b> secara deklaratif:
 * setiap endpoint dikunci hanya untuk role tertentu.</p>
 *
 * <p>Strategi keamanan:</p>
 * <ul>
 *   <li><b>Web pages (/, /auth/**, /citizen/**, /admin/**, /collector/**)</b>:
 *       Form login + Session (lebih natural untuk Thymeleaf)</li>
 *   <li><b>REST API (/api/**)</b>: Stateless JWT via Bearer token</li>
 * </ul>
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final AdminLogService adminLogService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, 
                          UserDetailsService userDetailsService,
                          AdminLogService adminLogService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.adminLogService = adminLogService;
    }

    // ==================== Filter Chain 1: REST API (Stateless JWT) ====================

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — terbuka untuk semua
                .requestMatchers("/api/v1/auth/**").permitAll()
                // IoT endpoint — cukup dengan API key (divalidasi di controller)
                .requestMatchers("/api/v1/iot/**").permitAll()
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
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ==================== Filter Chain 2: Web Pages (Session + Form Login) ====================

    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/internal/**"))
            .authorizeHttpRequests(auth -> auth
                // Halaman publik
                .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**",
                                 "/webjars/**", "/favicon.ico").permitAll()
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

                    // Redirect berdasarkan role setelah login sukses
                    String redirectUrl = "/";
                    for (var authority : authorities) {
                        redirectUrl = switch (authority.getAuthority()) {
                            case "ROLE_ADMIN"     -> "/admin/dashboard";
                            case "ROLE_CITIZEN"   -> "/citizen/dashboard";
                            case "ROLE_COLLECTOR" -> "/collector/dashboard";
                            default -> "/";
                        };
                        break;
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
            );

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
