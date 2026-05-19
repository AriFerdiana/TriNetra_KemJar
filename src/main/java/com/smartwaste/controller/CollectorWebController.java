package com.smartwaste.controller;

import com.smartwaste.dto.response.WasteDepositResponse;
import com.smartwaste.entity.enums.DepositStatus;
import com.smartwaste.repository.CollectorRepository;
import com.smartwaste.service.WasteDepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller MVC untuk dashboard Petugas (Collector).
 */
@Controller
@RequestMapping("/collector")
@RequiredArgsConstructor
public class CollectorWebController {

    private final WasteDepositService depositService;
    private final CollectorRepository collectorRepository;
    private final com.smartwaste.service.impl.PdfExportService pdfExportService;
    private final com.smartwaste.repository.WasteCategoryRepository categoryRepository;
    private final com.smartwaste.repository.CitizenRepository citizenRepository;
    private final com.smartwaste.repository.CollectorNotificationRepository notificationRepository;
    private final com.smartwaste.repository.SmartBinRepository smartBinRepository;
    private final com.smartwaste.repository.FieldReportRepository fieldReportRepository;
    private final com.smartwaste.service.impl.FileStorageService fileStorageService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('COLLECTOR')")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        
        // Defaults aman jika collector tidak ditemukan
        model.addAttribute("collectorName", "Petugas");
        model.addAttribute("vehicleNumber", "-");
        model.addAttribute("assignedArea", "-");
        model.addAttribute("available", true);
        model.addAttribute("totalConfirmed", 0L);
        model.addAttribute("totalRejected", 0L);
        model.addAttribute("todayWeight", 0.0);
        // Default untuk vehicle tracker — WAJIB di-set sebelum ifPresent agar Thymeleaf
        // tidak melempar PropertyAccessException jika collector tidak ditemukan
        model.addAttribute("currentLoad", 0.0);
        model.addAttribute("maxCapacity", 500.0);
        model.addAttribute("phone", "");

        // Load data profil collector
        collectorRepository.findByEmail(userDetails.getUsername())
                .ifPresent(collector -> {
                    model.addAttribute("collectorName", collector.getName());
                    model.addAttribute("vehicleNumber", collector.getVehicleNumber() != null ? collector.getVehicleNumber() : "-");
                    model.addAttribute("assignedArea", collector.getAssignedArea() != null ? collector.getAssignedArea() : "-");
                    model.addAttribute("available", collector.isAvailable());
                    model.addAttribute("currentLoad", collector.getCurrentLoadKg() != null ? collector.getCurrentLoadKg() : 0.0);
                    model.addAttribute("maxCapacity", collector.getMaxCapacityKg() != null ? collector.getMaxCapacityKg() : 500.0);
                    model.addAttribute("phone", collector.getPhone());
                    
                    // Stats
                    try {
                        long totalConfirmed = depositService.countByCollectorAndStatus(collector, com.smartwaste.entity.enums.DepositStatus.CONFIRMED);
                        long totalRejected  = depositService.countByCollectorAndStatus(collector, com.smartwaste.entity.enums.DepositStatus.REJECTED);
                        model.addAttribute("totalConfirmed", totalConfirmed);
                        model.addAttribute("totalRejected", totalRejected);
                    } catch (Exception e) {
                        // biarkan default 0
                    }
                });

        // Load setoran PENDING
        try {
            Page<WasteDepositResponse> pendingDeposits = depositService.getPendingDeposits(
                    PageRequest.of(page, 50, Sort.by("createdAt").descending()));
            model.addAttribute("pendingDeposits", pendingDeposits);
        } catch (Exception e) {
            model.addAttribute("pendingDeposits", org.springframework.data.domain.Page.empty());
        }

        // Load riwayat setoran — sort by createdAt (bukan confirmedAt agar null-safe)
        try {
            Page<WasteDepositResponse> historyDeposits = depositService.getCollectorHistory(
                    userDetails.getUsername(), PageRequest.of(page, 50, Sort.by("createdAt").descending()));
            model.addAttribute("historyDeposits", historyDeposits);
        } catch (Exception e) {
            model.addAttribute("historyDeposits", org.springframework.data.domain.Page.empty());
        }

        // Load total berat hari ini untuk gamifikasi
        try {
            double todayWeight = depositService.getTodayWeightByCollector(userDetails.getUsername());
            model.addAttribute("todayWeight", todayWeight);
        } catch (Exception e) {
            // biarkan default 0.0
        }

        // Load kategori stats
        try {
            java.util.List<Object[]> categoryStats = depositService.getCategoryStatsByCollector(userDetails.getUsername());
            model.addAttribute("categoryStats", categoryStats);
        } catch (Exception e) {
            model.addAttribute("categoryStats", java.util.List.of());
        }

        // Load all active categories for manual deposit form
        try {
            model.addAttribute("categories", categoryRepository.findByActiveTrue());
        } catch (Exception e) {
            model.addAttribute("categories", java.util.List.of());
        }

        // Load notifications with defaults
        model.addAttribute("notifications", java.util.List.of());
        model.addAttribute("unreadNotifCount", 0L);
        
        try {
            collectorRepository.findByEmail(userDetails.getUsername()).ifPresent(collector -> {
                var notifs = notificationRepository.findByCollectorIdOrBroadcast(collector.getId());
                model.addAttribute("notifications", notifs);
                model.addAttribute("unreadNotifCount", notificationRepository.countUnreadByCollectorId(collector.getId()));
            });
        } catch (Exception e) {
            System.err.println("Error loading notifications: " + e.getMessage());
        }

        // Feature D: Leaderboard
        try {
            model.addAttribute("leaderboard", collectorRepository.getCollectorLeaderboard());
            
            // Fetch confirmed deposits for history tab
            collectorRepository.findByEmail(userDetails.getUsername()).ifPresent(collector -> {
                model.addAttribute("confirmedDeposits", depositService.getConfirmedByCollector(collector.getId()));
                model.addAttribute("totalConfirmed", depositService.countByCollectorAndStatus(collector, DepositStatus.CONFIRMED));
                model.addAttribute("totalRejected", depositService.countByCollectorAndStatus(collector, DepositStatus.REJECTED));
            });
        } catch (Exception e) {
            model.addAttribute("leaderboard", java.util.List.of());
            model.addAttribute("confirmedDeposits", java.util.List.of());
        }

        // Feature E: Advanced Analytics
        try {
            model.addAttribute("totalWeightAllTime", depositService.getTotalWeightByCollector(userDetails.getUsername()));
            model.addAttribute("totalPointsDisbursed", depositService.getTotalPointsByCollector(userDetails.getUsername()));
            model.addAttribute("totalCitizensServed", depositService.countUniqueCitizensServedByCollector(userDetails.getUsername()));
            model.addAttribute("collectionTrend", depositService.getCollectionTrendByCollector(userDetails.getUsername()));
            
            // Calculate Efficiency Rate
            Object confirmedObj = model.getAttribute("totalConfirmed");
            Object rejectedObj = model.getAttribute("totalRejected");
            long c = (confirmedObj instanceof Long) ? (Long) confirmedObj : 0L;
            long r = (rejectedObj instanceof Long) ? (Long) rejectedObj : 0L;
            long total = c + r;
            double efficiency = total > 0 ? (double) c / total * 100 : 0;
            model.addAttribute("efficiencyRate", efficiency);
            
        } catch (Exception e) {
            model.addAttribute("totalWeightAllTime", 0.0);
            model.addAttribute("totalPointsDisbursed", 0.0);
            model.addAttribute("totalCitizensServed", 0L);
            model.addAttribute("collectionTrend", java.util.List.of());
        }

        return "collector/dashboard";
    }

    @PostMapping("/toggle-availability")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> toggleAvailability(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            boolean[] newVal = {true};
            collectorRepository.findByEmail(userDetails.getUsername()).ifPresent(collector -> {
                collector.setAvailable(!collector.isAvailable());
                newVal[0] = collector.isAvailable();
                collectorRepository.save(collector);
            });
            return ResponseEntity.ok(java.util.Map.of("success", true, "available", newVal[0]));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }


    @PostMapping("/deposit/{id}/confirm")
    @PreAuthorize("hasRole('COLLECTOR')")
    public String confirmDeposit(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable String id,
                                 @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            String finalUrl = "";
            if (file != null && !file.isEmpty()) {
                finalUrl = fileStorageService.storeFile(file);
            }
            depositService.confirmDeposit(id, userDetails.getUsername(), finalUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Setoran berhasil dikonfirmasi dan poin telah disalurkan.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal konfirmasi: " + e.getMessage());
        }
        return "redirect:/collector/dashboard";
    }

    @PostMapping("/deposit/{id}/reject")
    @PreAuthorize("hasRole('COLLECTOR')")
    public String rejectDeposit(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable String id,
                                @RequestParam String reason,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            depositService.rejectDeposit(id, userDetails.getUsername(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Setoran berhasil ditolak.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal menolak setoran: " + e.getMessage());
        }
        return "redirect:/collector/dashboard";
    }

    /**
     * Endpoint JSON untuk polling jumlah setoran PENDING.
     * Digunakan oleh front-end untuk auto-refresh tanpa full page reload.
     */
    @GetMapping("/pending-count")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> getPendingCount() {
        try {
            long count = depositService.countPendingDeposits();
            return ResponseEntity.ok(java.util.Map.of("count", count, "ok", true));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("count", 0, "ok", false));
        }
    }

    /**
     * Export laporan harian petugas sebagai PDF.
     */
    @GetMapping("/export/daily-report")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<byte[]> exportDailyReport(@AuthenticationPrincipal UserDetails userDetails) {
        return collectorRepository.findByEmail(userDetails.getUsername())
                .map(collector -> {
                    byte[] pdf = pdfExportService.exportCollectorDailyReport(collector);
                    String filename = "Laporan_" + collector.getName().replace(" ", "_") + "_" + java.time.LocalDate.now() + ".pdf";
                    return ResponseEntity.ok()
                            .header("Content-Type", "application/pdf")
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Export seluruh riwayat petugas sebagai PDF.
     */
    @GetMapping("/export/history")
    @PreAuthorize("hasRole('COLLECTOR')")
    public ResponseEntity<byte[]> exportFullHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return collectorRepository.findByEmail(userDetails.getUsername())
                .map(collector -> {
                    byte[] pdf = pdfExportService.exportCollectorFullHistory(collector);
                    String filename = "Riwayat_Lengkap_" + collector.getName().replace(" ", "_") + ".pdf";
                    return ResponseEntity.ok()
                            .header("Content-Type", "application/pdf")
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mencatat setoran manual oleh petugas (drop-off langsung).
     */
    @PostMapping("/deposit/manual")
    @PreAuthorize("hasRole('COLLECTOR')")
    public String createManualDeposit(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestParam String citizenId,
                                      @RequestParam String categoryId,
                                      @RequestParam double weightKg,
                                      @RequestParam(required = false) String notes,
                                      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            depositService.createManualDeposit(userDetails.getUsername(), citizenId, categoryId, weightKg, notes);
            ra.addFlashAttribute("successMessage", "Setoran manual berhasil dicatat dan poin telah dikreditkan.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Gagal mencatat setoran manual: " + e.getMessage());
        }
        return "redirect:/collector/dashboard";
    }

    /**
     * Search warga via AJAX untuk form setoran manual.
     */
    @GetMapping("/search-citizens")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> searchCitizens(@RequestParam String q) {
        // Minimal 2 karakter agar tidak membebani DB dengan query wildcard yang terlalu lebar
        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(java.util.List.of());
        }
        java.util.List<java.util.Map<String, String>> result = citizenRepository.searchCitizens(q, true,
                org.springframework.data.domain.PageRequest.of(0, 8)).stream()
                .map(c -> java.util.Map.of(
                        "id", c.getId() != null ? c.getId() : "",
                        "name", c.getName() != null ? c.getName() : "",
                        "nik", c.getNik() != null ? c.getNik() : ""
                ))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Mark notification as read.
     */
    @PostMapping("/notifications/{id}/read")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> markNotifRead(@PathVariable String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setReadStatus(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    @PostMapping("/profile/edit")
    @PreAuthorize("hasRole('COLLECTOR')")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String name,
                              @RequestParam String phone,
                              @RequestParam String vehicleNumber,
                              @RequestParam double maxCapacityKg,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            collectorRepository.findByEmail(userDetails.getUsername()).ifPresent(collector -> {
                collector.setName(name);
                collector.setPhone(phone);
                collector.setVehicleNumber(vehicleNumber);
                collector.setMaxCapacityKg(maxCapacityKg);
                collectorRepository.save(collector);
            });
            ra.addFlashAttribute("successMessage", "Profil berhasil diperbarui.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Gagal update profil: " + e.getMessage());
        }
        return "redirect:/collector/dashboard";
    }

    @PostMapping("/reset-load")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> resetLoad(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            collectorRepository.findByEmail(userDetails.getUsername()).ifPresent(collector -> {
                collector.setCurrentLoadKg(0.0);
                collectorRepository.save(collector);
            });
            return ResponseEntity.ok(java.util.Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all smart bins for the map overlay.
     */
    @GetMapping("/smart-bins")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.List<com.smartwaste.entity.SmartBin>> getSmartBins() {
        return ResponseEntity.ok(smartBinRepository.findAll());
    }

    /**
     * Submit a field report (broken bin, illegal dumping, etc.)
     */
    @PostMapping("/report-issue")
    @PreAuthorize("hasRole('COLLECTOR')")
    public String reportIssue(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String title,
                              @RequestParam String description,
                              @RequestParam(required = false) String location,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            collectorRepository.findByEmail(userDetails.getUsername()).ifPresent(collector -> {
                com.smartwaste.entity.FieldReport report = new com.smartwaste.entity.FieldReport();
                report.setCollector(collector);
                report.setTitle(title);
                report.setDescription(description);
                report.setLocation(location);
                fieldReportRepository.save(report);
            });
            ra.addFlashAttribute("successMessage", "Laporan lapangan berhasil dikirim.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Gagal mengirim laporan: " + e.getMessage());
        }
        return "redirect:/collector/dashboard";
    }

    /**
     * Check if there are new pending tasks since the last view.
     */
    @GetMapping("/check-new-tasks")
    @PreAuthorize("hasRole('COLLECTOR')")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> checkNewTasks(@RequestParam long lastCount) {
        long currentCount = depositService.countPendingDeposits();
        boolean hasNew = currentCount > lastCount;
        return ResponseEntity.ok(java.util.Map.of("hasNew", hasNew, "currentCount", currentCount));
    }
}

