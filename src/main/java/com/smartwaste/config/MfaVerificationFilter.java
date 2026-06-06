package com.smartwaste.config;

import com.smartwaste.entity.User;
import com.smartwaste.service.MfaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter untuk memastikan bahwa user yang mengaktifkan MFA telah melakukan verifikasi.
 * Jika MFA aktif namun belum diverifikasi (session mfa_verified = false), 
 * user akan terus di-redirect ke halaman verifikasi.
 */
@Component
public class MfaVerificationFilter extends OncePerRequestFilter {

    private final MfaService mfaService;

    public MfaVerificationFilter(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
            
        String path = request.getRequestURI();
        
        // Lewati filter untuk endpoint publik, statik, dan MFA itu sendiri
        if (path.startsWith("/auth/") || path.startsWith("/css/") || 
            path.startsWith("/js/") || path.startsWith("/images/") || 
            path.startsWith("/api/") || path.equals("/mfa/verify") ||
            path.equals("/mfa/disable") || path.equals("/") || path.equals("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            
            // Cek apakah MFA diaktifkan untuk user ini
            if (mfaService.isMfaEnabled(email)) {
                // Cek status verifikasi di session
                Boolean mfaVerified = (Boolean) request.getSession().getAttribute("MFA_VERIFIED");
                
                // Jika belum diverifikasi
                if (mfaVerified == null || !mfaVerified) {
                    // Redirect ke halaman verifikasi
                    response.sendRedirect(request.getContextPath() + "/mfa/verify?email=" + email);
                    return;
                }
            } else {
                // Jika MFA belum aktif, paksa user untuk setup MFA
                if (!path.equals("/mfa/setup") && !path.equals("/mfa/setup/enable")) {
                    response.sendRedirect(request.getContextPath() + "/mfa/setup");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
