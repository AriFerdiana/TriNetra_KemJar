package com.smartwaste.controller;

import com.smartwaste.service.MfaService;
import com.smartwaste.service.SecurityAuditService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller untuk halaman setup dan verifikasi MFA (Multi-Factor Authentication).
 *
 * <p>Endpoint:</p>
 * <ul>
 *   <li>GET /mfa/setup — Halaman setup MFA (tampilkan QR Code)</li>
 *   <li>POST /mfa/setup/enable — Konfirmasi kode OTP dan aktifkan MFA</li>
 *   <li>GET /mfa/verify — Halaman verifikasi OTP saat login</li>
 *   <li>POST /mfa/verify — Proses verifikasi OTP saat login</li>
 *   <li>POST /mfa/disable — Nonaktifkan MFA</li>
 * </ul>
 */
@Controller
@RequestMapping("/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;
    private final SecurityAuditService auditService;

    /**
     * Halaman setup MFA — tampilkan QR Code untuk di-scan Google Authenticator.
     */
    @GetMapping("/setup")
    public String setupMfaPage(Authentication auth, Model model) {
        String email = auth.getName();
        try {
            String qrCode = mfaService.generateQrCodeDataUri(email);
            boolean isEnabled = mfaService.isMfaEnabled(email);
            model.addAttribute("qrCodeDataUri", qrCode);
            model.addAttribute("mfaEnabled", isEnabled);
            model.addAttribute("userEmail", email);
            
            // Add correct dashboard URL based on role
            String dashboardUrl = "/";
            for (var authority : auth.getAuthorities()) {
                switch (authority.getAuthority()) {
                    case "ROLE_ADMIN": dashboardUrl = "/admin/dashboard"; break;
                    case "ROLE_CITIZEN": dashboardUrl = "/citizen/dashboard"; break;
                    case "ROLE_COLLECTOR": dashboardUrl = "/collector/dashboard"; break;
                }
            }
            model.addAttribute("dashboardUrl", dashboardUrl);
            
        } catch (QrGenerationException e) {
            model.addAttribute("errorMessage", "Gagal generate QR Code. Silakan coba lagi.");
        }
        return "auth/setup-mfa";
    }

    /**
     * Proses aktivasi MFA — verifikasi kode OTP pertama kali untuk konfirmasi.
     */
    @PostMapping("/setup/enable")
    public String enableMfa(Authentication auth,
                            @RequestParam String otpCode,
                            jakarta.servlet.http.HttpServletRequest request,
                            jakarta.servlet.http.HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String email = auth.getName();
        String ip = request.getRemoteAddr();

        boolean success = mfaService.enableMfa(email, otpCode);
        if (success) {
            session.setAttribute("MFA_VERIFIED", true);
            auditService.logMfaEvent("MFA_ENABLED", email, ip);
            redirectAttributes.addFlashAttribute("successMessage",
                "✅ MFA berhasil diaktifkan! Akun Anda kini lebih aman.");
        } else {
            auditService.logMfaEvent("MFA_ENABLE_FAILED", email, ip);
            redirectAttributes.addFlashAttribute("errorMessage",
                "❌ Kode OTP tidak valid. Pastikan waktu HP Anda sinkron dan coba lagi.");
        }
        return "redirect:/mfa/setup";
    }

    /**
     * Halaman verifikasi OTP — ditampilkan setelah user login dengan password benar.
     */
    @GetMapping("/verify")
    public String verifyMfaPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "auth/verify-mfa";
    }

    /**
     * Proses verifikasi OTP saat login.
     */
    @PostMapping("/verify")
    public String verifyMfa(@RequestParam String email,
                            @RequestParam String otpCode,
                            jakarta.servlet.http.HttpServletRequest request,
                            jakarta.servlet.http.HttpSession session,
                            org.springframework.security.core.Authentication auth,
                            RedirectAttributes redirectAttributes) {
        boolean valid = mfaService.verifyCode(email, otpCode);

        if (valid) {
            session.setAttribute("MFA_VERIFIED", true);
            auditService.logMfaEvent("MFA_SUCCESS", email, request.getRemoteAddr());
            
            // Redirect ke halaman sebelumnya jika ada (HANYA JIKA REQUEST AWAL ADALAH GET)
            org.springframework.security.web.savedrequest.SavedRequest savedRequest = 
                (org.springframework.security.web.savedrequest.SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
                
            if (savedRequest != null && savedRequest.getMethod().equalsIgnoreCase("GET")) {
                return "redirect:" + savedRequest.getRedirectUrl();
            }
            
            // Jika tidak ada saved request, redirect berdasarkan role
            if (auth != null) {
                for (var authority : auth.getAuthorities()) {
                    switch (authority.getAuthority()) {
                        case "ROLE_ADMIN": return "redirect:/admin/dashboard";
                        case "ROLE_CITIZEN": return "redirect:/citizen/dashboard";
                        case "ROLE_COLLECTOR": return "redirect:/collector/dashboard";
                    }
                }
            }
            
            return "redirect:/";
        } else {
            auditService.logMfaEvent("MFA_FAILED", email, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("errorMessage",
                "Kode OTP salah atau sudah kadaluarsa. Silakan coba lagi.");
            return "redirect:/mfa/verify?email=" + email;
        }
    }

    /**
     * Nonaktifkan MFA untuk user yang sedang login.
     */
    @PostMapping("/disable")
    public String disableMfa(Authentication auth, jakarta.servlet.http.HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String email = auth.getName();
        mfaService.disableMfa(email);
        auditService.logMfaEvent("MFA_DISABLED", email, request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("successMessage", "MFA berhasil dinonaktifkan.");
        return "redirect:/mfa/setup";
    }
}
