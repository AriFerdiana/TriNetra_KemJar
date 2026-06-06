package com.smartwaste.controller;

import com.smartwaste.service.impl.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller untuk halaman-halaman publik (landing page, login, register).
 * Menggunakan @Controller (bukan @RestController) untuk mengembalikan nama view Thymeleaf.
 */
@Controller
public class WebController {

    private final PasswordResetService passwordResetService;

    public WebController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/auth/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String timeout,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Email atau password salah. Silakan coba lagi.");
        }
        if (logout != null) {
            model.addAttribute("successMsg", "Anda berhasil logout. Sampai jumpa! 👋");
        }
        if (timeout != null) {
            model.addAttribute("errorMsg", "Sesi Anda telah berakhir. Silakan coba lagi.");
        }
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String registerPage() {
        return "auth/register";
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/auth/access-denied", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String accessDenied() {
        return "auth/access-denied";
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                        RedirectAttributes redirectAttributes) {
        passwordResetService.createPasswordResetTokenForUser(email);
        redirectAttributes.addFlashAttribute("successMessage", "Jika email terdaftar, instruksi reset password telah dikirim.");
        return "redirect:/auth/login";
    }

    @GetMapping("/auth/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/auth/reset-password")
    public String processResetPassword(@RequestParam("token") String token, 
                                       @RequestParam("newPassword") String newPassword,
                                       RedirectAttributes redirectAttributes) {
        boolean success = passwordResetService.resetPassword(token, newPassword);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Password berhasil diubah. Silakan login.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Token tidak valid atau sudah kedaluwarsa.");
        }
        return "redirect:/auth/login";
    }

    @PostMapping("/auth/logout")
    public String manualLogout(jakarta.servlet.http.HttpServletRequest request, 
                               jakarta.servlet.http.HttpServletResponse response, 
                               org.springframework.security.core.Authentication auth) {
        if (auth != null) {
            new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/auth/login?logout=true";
    }
}
