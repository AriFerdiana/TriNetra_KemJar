package com.smartwaste.controller;

import com.smartwaste.entity.SecurityLog;
import com.smartwaste.repository.SecurityLogRepository;
import com.smartwaste.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/security-audit")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityAuditWebController {

    private final SecurityLogRepository securityLogRepository;
    private final ChatbotService chatbotService;

    @Autowired
    public SecurityAuditWebController(SecurityLogRepository securityLogRepository, ChatbotService chatbotService) {
        this.securityLogRepository = securityLogRepository;
        this.chatbotService = chatbotService;
    }

    @GetMapping
    public String viewDashboard(Model model) {
        // Ambil 50 log terbaru untuk ditampilkan di tabel
        List<SecurityLog> logs = securityLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50));
        model.addAttribute("logs", logs);
        return "admin/security-audit";
    }

    @PostMapping("/analyze")
    @ResponseBody
    public String analyzeWithAi() {
        // Ambil 20 log terbaru untuk dianalisis AI (agar tidak melebihi token limit)
        List<SecurityLog> logs = securityLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));
        if (logs.isEmpty()) {
            return "Belum ada data log keamanan untuk dianalisis saat ini.";
        }

        // Format log menjadi teks mentah agar mudah dibaca oleh Mistral AI
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String logsText = logs.stream()
                .map(log -> String.format("[%s] IP: %s | Event: %s | Detail: %s",
                        log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "N/A",
                        log.getIpAddress(),
                        log.getEventType(),
                        log.getDetail()))
                .collect(Collectors.joining("\n"));

        // Panggil Mistral AI
        return chatbotService.analyzeSecurityThreats(logsText);
    }
}
