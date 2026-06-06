package com.smartwaste.config;

import com.smartwaste.service.SecurityAuditService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter Rate Limiting — Perlindungan Brute Force.
 *
 * <p>Setiap IP address dibatasi maksimum <b>10 request per menit</b> untuk
 * endpoint login. Jika melebihi batas, server mengembalikan HTTP 429 (Too Many Requests)
 * dan mencatat peringatan keamanan ke log Wazuh.</p>
 *
 * <p>Mendukung fallback: menggunakan Redis jika tersedia, jika tidak jatuh kembali
 * ke algoritma <b>Token Bucket</b> in-memory via library Bucket4j.</p>
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Endpoint yang diproteksi rate limiting
    private static final String[] PROTECTED_PATHS = {
        "/auth/login", "/api/v1/auth/login", "/api/v1/auth/register"
    };

    // Batas: 10 request per menit per IP
    private static final int MAX_REQUESTS = 10;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    // Penyimpanan bucket per IP (thread-safe) untuk fallback in-memory
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final SecurityAuditService auditService;
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public RateLimitingFilter(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getServletPath();

        // Hanya terapkan pada endpoint yang dilindungi
        if (!isProtectedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        boolean allowed = false;

        try {
            if (redisTemplate != null) {
                // Gunakan Redis
                String key = "rate_limit:" + requestPath + ":" + clientIp;
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, REFILL_DURATION);
                }
                allowed = (count != null && count <= MAX_REQUESTS);
            } else {
                allowed = tryConsumeInMemory(clientIp);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable or error, falling back to local memory bucket: {}", e.getMessage());
            allowed = tryConsumeInMemory(clientIp);
        }

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            // Rate limit terlampaui
            auditService.logSecurityEvent("BRUTE_FORCE_DETECTED", clientIp,
                "Endpoint: " + requestPath + " | Rate limit exceeded (>" + MAX_REQUESTS + " req/min)");

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                {"status":429,"error":"Too Many Requests",
                 "message":"Terlalu banyak percobaan. Silakan tunggu 1 menit."}
                """);
        }
    }

    private boolean tryConsumeInMemory(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);
        return bucket.tryConsume(1);
    }

    /** Buat bucket baru untuk IP yang belum dikenal. */
    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(MAX_REQUESTS, Refill.greedy(MAX_REQUESTS, REFILL_DURATION));
        return Bucket.builder().addLimit(limit).build();
    }

    /** Cek apakah path request termasuk endpoint yang diproteksi. */
    private boolean isProtectedPath(String path) {
        for (String p : PROTECTED_PATHS) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    /**
     * Ekstrak IP asli client, mendukung request yang datang via Nginx Reverse Proxy.
     * Nginx akan meneruskan IP asli di header X-Forwarded-For.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
