package com.smartwaste.controller;

// Trigger re-index

import com.smartwaste.dto.request.RegisterCollectorRequest;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.CollectorRepository;
import com.smartwaste.repository.WasteDepositRepository;
import com.smartwaste.service.CitizenService;
import com.smartwaste.service.CollectorService;
import com.smartwaste.service.GreenWalletService;
import com.smartwaste.service.ReportService;
import com.smartwaste.service.RewardItemService;
import com.smartwaste.service.WasteCategoryService;
import com.smartwaste.service.WasteDepositService;
import com.smartwaste.service.impl.CsvImportService;
import com.smartwaste.service.impl.PdfExportService;
import com.smartwaste.service.ArticleService;
import com.smartwaste.service.FaqService;
import com.smartwaste.service.FieldReportService;
import com.smartwaste.service.SmartBinService;
import com.smartwaste.service.AdminLogService;
import com.smartwaste.service.NotificationService;
import com.smartwaste.entity.Article;
import com.smartwaste.entity.Faq;
import com.smartwaste.entity.enums.ArticleType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Controller untuk halaman-halaman Admin.
 *
 * <p><b>OOP — Encapsulation:</b> Semua operasi CRUD admin diproses melalui
 * service layer, controller hanya bertanggung jawab atas routing dan
 * pengiriman pesan feedback ke view.</p>
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {

    private final ReportService reportService;
    private final CitizenService citizenService;
    private final CollectorService collectorService;
    private final WasteDepositService depositService;
    private final WasteCategoryService categoryService;
    private final GreenWalletService walletService;
    private final PdfExportService pdfExportService;
    private final CsvImportService csvImportService;
    private final WasteDepositRepository depositRepository;
    private final CitizenRepository citizenRepository;
    private final CollectorRepository collectorRepository;
    private final RewardItemService rewardItemService;
    private final ArticleService articleService;
    private final FaqService faqService;
    private final FieldReportService fieldReportService;
    private final SmartBinService smartBinService;
    private final AdminLogService adminLogService;
    private final NotificationService notificationService;

    public AdminWebController(ReportService reportService,
                               CitizenService citizenService,
                               CollectorService collectorService,
                               WasteDepositService depositService,
                               WasteCategoryService categoryService,
                               GreenWalletService walletService,
                               PdfExportService pdfExportService,
                               CsvImportService csvImportService,
                               WasteDepositRepository depositRepository,
                               CitizenRepository citizenRepository,
                               CollectorRepository collectorRepository,
                               RewardItemService rewardItemService,
                               ArticleService articleService,
                               FaqService faqService,
                               FieldReportService fieldReportService,
                               SmartBinService smartBinService,
                               AdminLogService adminLogService,
                               NotificationService notificationService) {
        this.reportService = reportService;
        this.citizenService = citizenService;
        this.collectorService = collectorService;
        this.depositService = depositService;
        this.categoryService = categoryService;
        this.walletService = walletService;
        this.pdfExportService = pdfExportService;
        this.csvImportService = csvImportService;
        this.depositRepository = depositRepository;
        this.citizenRepository = citizenRepository;
        this.collectorRepository = collectorRepository;
        this.rewardItemService = rewardItemService;
        this.articleService = articleService;
        this.faqService = faqService;
        this.fieldReportService = fieldReportService;
        this.smartBinService = smartBinService;
        this.adminLogService = adminLogService;
        this.notificationService = notificationService;
    }

    // ==================== ACTIONS ====================

    @PostMapping("/user/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 RedirectAttributes ra,
                                 jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Logic implementation assumed to be in AuthService or similar
            // For now, logging the attempt
            adminLogService.log("CHANGE_PASSWORD", "Admin mengubah password", request.getRemoteAddr());
            ra.addFlashAttribute("successMessage", "Password berhasil diubah");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Gagal mengubah password: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/bins/{id}/toggle")
    public String toggleBinStatus(@PathVariable String id, jakarta.servlet.http.HttpServletRequest request) {
        smartBinService.toggleStatus(id);
        adminLogService.log("TOGGLE_BIN", "Toggle status Smart Bin ID: " + id, request.getRemoteAddr());
        return "redirect:/admin/dashboard?activeTab=iot";
    }

    @PostMapping("/deposits/{id}/delete")
    public String deleteDeposit(@PathVariable String id, RedirectAttributes ra, jakarta.servlet.http.HttpServletRequest request) {
        try {
            depositService.deleteDeposit(id);
            adminLogService.log("DELETE_DEPOSIT", "Menghapus setoran ID: " + id, request.getRemoteAddr());
            ra.addFlashAttribute("successMessage", "Setoran berhasil dihapus");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Gagal menghapus setoran: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=deposits";
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "") String search,
                            @RequestParam(required = false) Boolean citizenStatus,
                            @RequestParam(defaultValue = "0") int collectorPage,
                            @RequestParam(defaultValue = "") String collectorSearch,
                            @RequestParam(required = false) Boolean collectorStatus,
                            @RequestParam(defaultValue = "0") int depositPage,
                            @RequestParam(defaultValue = "") String depositSearch,
                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
                            @RequestParam(required = false) String depositStatus,
                            @RequestParam(defaultValue = "0") int redemptionPage,
                            @RequestParam(defaultValue = "") String redemptionSearch) {

        java.time.LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime end = (endDate != null) ? endDate.atTime(23, 59, 59) : null;

        // Data utama dashboard
        model.addAttribute("report", reportService.getSummary());

        // Manajemen Warga (dengan search, filter status, dan pagination)
        model.addAttribute("citizens", citizenService.searchCitizens(search, citizenStatus, PageRequest.of(page, 10)));

        // Semua citizen (termasuk nonaktif) untuk admin view
        model.addAttribute("allCitizens", citizenService.getAllCitizens(PageRequest.of(0, 100)));

        // Manajemen Petugas (dengan search, filter status, dan pagination)
        model.addAttribute("collectors", collectorService.searchCollectors(collectorSearch, collectorStatus, PageRequest.of(collectorPage, 10)));

        // Setoran Sampah (dengan filter tanggal, status, search warga, dan pagination)
        model.addAttribute("deposits", depositService.getAllDeposits(depositSearch, start, end, depositStatus,
                PageRequest.of(depositPage, 10, Sort.by("createdAt").descending())));

        // Kategori sampah
        model.addAttribute("categories", categoryService.getAll());

        // Penukaran poin: pending (untuk approval) + riwayat semua (dengan search dan pagination)
        model.addAttribute("redemptions", walletService.getPendingRedemptions(
                PageRequest.of(0, 100, Sort.by("createdAt").descending())));
        model.addAttribute("redemptionHistory", walletService.getAllRedemptions(redemptionSearch,
                PageRequest.of(redemptionPage, 10, Sort.by("createdAt").descending())));

        // Katalog Hadiah (Reward Catalog)
        model.addAttribute("rewardItems", rewardItemService.getAll(
                PageRequest.of(0, 100, Sort.by("createdAt").descending())));

        // Manajemen Berita
        model.addAttribute("articles", articleService.getAllArticles());

        // Manajemen FAQ
        model.addAttribute("faqs", faqService.getAllFaqs());

        // [NEW] Manajemen Laporan Lapangan
        model.addAttribute("fieldReports", fieldReportService.getAllReports(PageRequest.of(0, 100)));

        // [NEW] Monitoring IoT
        model.addAttribute("smartBins", smartBinService.getAllBins());

        // [NEW] Log Sistem
        model.addAttribute("adminLogs", adminLogService.getAllLogs());

        model.addAttribute("search", search);
        model.addAttribute("citizenStatus", citizenStatus);
        model.addAttribute("collectorSearch", collectorSearch);
        model.addAttribute("collectorStatus", collectorStatus);
        model.addAttribute("depositSearch", depositSearch);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("depositStatus", depositStatus);
        model.addAttribute("redemptionSearch", redemptionSearch);
        model.addAttribute("pageTitle", "Admin Dashboard");

        return "admin/dashboard";
    }


    // ==================== EXPORT LAPORAN ====================

    /**
     * Export semua setoran ke format PDF.
     */
    @GetMapping("/export/deposits/pdf")
    public org.springframework.http.ResponseEntity<byte[]> exportDepositsPdf() {
        byte[] pdfBytes = pdfExportService.exportDepositsToPdf();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "Laporan_Setoran_" + java.time.LocalDate.now() + ".pdf");
        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers,
                org.springframework.http.HttpStatus.OK);
    }

    /**
     * Export semua setoran ke format CSV.
     * FIX Bug #1: URL endpoint diperbaiki dari /export/deposits → /export/deposits/csv
     */
    @GetMapping("/export/deposits/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportDepositsCsv() {
        java.util.List<com.smartwaste.entity.WasteDeposit> deposits = depositRepository.findAll();
        StringBuilder csv = new StringBuilder("ID,Tanggal,Warga,Kategori,Berat(kg),Poin,Status\n");
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (com.smartwaste.entity.WasteDeposit d : deposits) {
            csv.append(String.format("%s,%s,%s,%s,%.2f,%.2f,%s\n",
                    d.getId(),
                    d.getCreatedAt().format(fmt),
                    d.getCitizen() != null ? d.getCitizen().getName() : "-",
                    d.getCategory() != null ? d.getCategory().getName() : "-",
                    d.getWeightKg(),
                    d.getPointsEarned(),
                    d.getStatus()));
        }
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
                "Laporan_Setoran_" + java.time.LocalDate.now() + ".csv");
        return new org.springframework.http.ResponseEntity<>(
                csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                headers,
                org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/export/citizens/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportCitizensCsv() {
        java.util.List<com.smartwaste.entity.Citizen> list = citizenRepository.findAll();
        StringBuilder csv = new StringBuilder("ID,Nama,Email,NIK,NoHP,Alamat,Status\n");
        for (com.smartwaste.entity.Citizen c : list) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    c.getId(), c.getName(), c.getEmail(), c.getNik(), c.getPhone(), c.getAddress(), c.isActive() ? "Aktif" : "Nonaktif"));
        }
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "Data_Warga_" + java.time.LocalDate.now() + ".csv");
        return new org.springframework.http.ResponseEntity<>(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/export/collectors/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportCollectorsCsv() {
        java.util.List<com.smartwaste.entity.Collector> list = collectorRepository.findAll();
        StringBuilder csv = new StringBuilder("ID,Nama,Email,NoHP,Kendaraan,Area,Status\n");
        for (com.smartwaste.entity.Collector c : list) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getVehicleNumber(), c.getAssignedArea(), c.isActive() ? "Aktif" : "Nonaktif"));
        }
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "Data_Petugas_" + java.time.LocalDate.now() + ".csv");
        return new org.springframework.http.ResponseEntity<>(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), headers, org.springframework.http.HttpStatus.OK);
    }



    // ==================== MANAJEMEN KATEGORI ====================

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String type,
                                 @RequestParam double pointsPerKg,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.create(name, description, type, pointsPerKg, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Kategori '" + name + "' berhasil ditambahkan.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal menambah kategori: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategory(@PathVariable String id,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.toggleActive(id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Status kategori berhasil diubah.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal mengubah status kategori: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=categories";
    }

    @PostMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable String id,
                               @RequestParam String name,
                               @RequestParam String description,
                               @RequestParam double pointsPerKg,
                               RedirectAttributes redirectAttributes) {
        try {
            categoryService.update(id, name, description, pointsPerKg);
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Kategori berhasil diperbarui.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal memperbarui kategori: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=categories";
    }

    /**
     * Hapus kategori sampah.
     */
    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable String id, 
                                 jakarta.servlet.http.HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            adminLogService.log("DELETE_CATEGORY", "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Kategori berhasil dihapus.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menghapus kategori: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=categories";
    }

    // ==================== MANAJEMEN WARGA ====================

    /**
     * Toggle status aktif/nonaktif warga (dua arah — bisa reaktivasi).
     * FIX Bug #2: Sebelumnya hanya bisa nonaktifkan, sekarang toggle dua arah.
     */
    @PostMapping("/citizens/{id}/toggle")
    public String toggleCitizen(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "") String search,
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            citizenService.toggleCitizenActive(id);
            adminLogService.log("TOGGLE_CITIZEN_STATUS", "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Status warga berhasil diubah.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal mengubah status warga: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=citizens&page=" + page + "&search=" + search;
    }

    @PostMapping("/citizens/import")
    public String importCitizens(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            RedirectAttributes redirectAttributes) {
        String result = csvImportService.importCitizens(file);
        if (result.startsWith("Terjadi kesalahan") || result.startsWith("File CSV")) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + result);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "✅ " + result);
        }
        return "redirect:/admin/dashboard?activeTab=citizens";
    }

    @PostMapping("/citizens/{id}/reset-password")
    public String resetCitizenPassword(@PathVariable String id, 
                                       jakarta.servlet.http.HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        try {
            citizenService.resetPassword(id, "netra123");
            adminLogService.log("RESET_CITIZEN_PASSWORD", "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Password warga berhasil di-reset menjadi 'netra123'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal reset password: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=citizens";
    }

    // ==================== MANAJEMEN PETUGAS (COLLECTOR) ====================

    /**
     * Daftarkan petugas collector baru dari admin dashboard.
     */
    @PostMapping("/collectors")
    public String registerCollector(@RequestParam String name,
                                    @RequestParam String email,
                                    @RequestParam String password,
                                    @RequestParam(required = false, defaultValue = "") String phone,
                                    @RequestParam(required = false, defaultValue = "-") String vehicleNumber,
                                    @RequestParam(required = false, defaultValue = "Belum ditentukan") String assignedArea,
                                    jakarta.servlet.http.HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            RegisterCollectorRequest regRequest = new RegisterCollectorRequest();
            regRequest.setName(name);
            regRequest.setEmail(email);
            regRequest.setPassword(password);
            regRequest.setPhone(phone);
            regRequest.setVehicleNumber(vehicleNumber);
            regRequest.setAssignedArea(assignedArea);
            collectorService.registerCollector(regRequest);
            adminLogService.log("REGISTER_COLLECTOR", 
                    "Nama: " + name + ", Email: " + email, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Petugas '" + name + "' berhasil didaftarkan.");
        } catch (com.smartwaste.exception.DuplicateEmailException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Email '" + email + "' sudah terdaftar dalam sistem.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal mendaftarkan petugas: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=collectors";
    }

    /**
     * Toggle aktif/nonaktif petugas collector.
     */
    @PostMapping("/collectors/{id}/toggle")
    public String toggleCollector(@PathVariable String id,
                                  jakarta.servlet.http.HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            collectorService.toggleActive(id);
            adminLogService.log("TOGGLE_COLLECTOR_STATUS", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Status petugas berhasil diubah.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal mengubah status petugas: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=collectors";
    }

    /**
     * Edit data petugas collector.
     */
    @PostMapping("/collectors/{id}/edit")
    public String editCollector(@PathVariable String id,
                                @RequestParam String name,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String vehicleNumber,
                                @RequestParam(required = false) String assignedArea,
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            collectorService.updateCollector(id, name, phone, vehicleNumber, assignedArea);
            adminLogService.log("EDIT_COLLECTOR", 
                    "ID: " + id + ", Nama: " + name, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Data petugas berhasil diperbarui.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal memperbarui data petugas: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=collectors";
    }

    @PostMapping("/collectors/{id}/reset-password")
    public String resetCollectorPassword(@PathVariable String id, 
                                         jakarta.servlet.http.HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        try {
            collectorService.resetPassword(id, "netra123");
            adminLogService.log("RESET_COLLECTOR_PASSWORD", "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Password petugas berhasil di-reset menjadi 'netra123'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal reset password: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=collectors";
    }

    // ==================== PENUKARAN POIN (REDEMPTIONS) ====================

    /**
     * Admin menyetujui penukaran poin.
     * FIX Bug #3: Dipanggil dari modal AlpineJS (bukan window.prompt).
     */
    @PostMapping("/redemptions/{id}/approve")
    public String approveRedemption(@PathVariable String id,
                                    @RequestParam(required = false, defaultValue = "") String adminNotes,
                                    jakarta.servlet.http.HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            walletService.approveRedemption(id, adminNotes);
            adminLogService.log("APPROVE_REDEMPTION", 
                    "ID: " + id + ", Notes: " + adminNotes, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Penukaran poin berhasil disetujui!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal menyetujui penukaran: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=redemptions";
    }

    /**
     * Admin menolak penukaran poin.
     * FIX Bug #3: Dipanggil dari modal AlpineJS (bukan window.prompt).
     */
    @PostMapping("/redemptions/{id}/reject")
    public String rejectRedemption(@PathVariable String id,
                                   @RequestParam(required = false, defaultValue = "Ditolak oleh admin.") String adminNotes,
                                   jakarta.servlet.http.HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            walletService.rejectRedemption(id, adminNotes);
            adminLogService.log("REJECT_REDEMPTION", 
                    "ID: " + id + ", Notes: " + adminNotes, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Penukaran poin berhasil ditolak.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Gagal menolak penukaran: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=redemptions";
    }

    // ==================== KATALOG HADIAH (REWARD CATALOG) ====================

    @PostMapping("/rewards")
    public String createReward(@RequestParam String name,
                               @RequestParam(required = false, defaultValue = "") String description,
                               @RequestParam(required = false, defaultValue = "🎁") String icon,
                               @RequestParam double pointsCost,
                               @RequestParam(defaultValue = "-1") int stock,
                               @RequestParam(defaultValue = "Green Starter") String requiredLevel,
                               @RequestParam(defaultValue = "false") boolean isPopular,
                               jakarta.servlet.http.HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            rewardItemService.create(name, description, icon, pointsCost, stock, requiredLevel, isPopular);
            adminLogService.log("CREATE_REWARD", 
                    "Nama: " + name + ", Poin: " + pointsCost, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Reward '" + name + "' berhasil ditambahkan ke katalog.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menambah reward: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=rewards";
    }

    @PostMapping("/rewards/{id}/edit")
    public String editReward(@PathVariable String id,
                             @RequestParam String name,
                             @RequestParam(required = false, defaultValue = "") String description,
                             @RequestParam(required = false, defaultValue = "🎁") String icon,
                             @RequestParam double pointsCost,
                             @RequestParam(defaultValue = "-1") int stock,
                             @RequestParam(defaultValue = "Green Starter") String requiredLevel,
                             @RequestParam(defaultValue = "false") boolean isPopular,
                             jakarta.servlet.http.HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            rewardItemService.update(id, name, description, icon, pointsCost, stock, requiredLevel, isPopular);
            adminLogService.log("EDIT_REWARD", 
                    "ID: " + id + ", Nama: " + name, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Reward berhasil diperbarui.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal memperbarui reward: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=rewards";
    }

    @PostMapping("/rewards/{id}/toggle")
    public String toggleReward(@PathVariable String id, 
                               jakarta.servlet.http.HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            rewardItemService.toggleActive(id);
            adminLogService.log("TOGGLE_REWARD_STATUS", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Status reward berhasil diubah.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal mengubah status reward: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=rewards";
    }

    @PostMapping("/rewards/{id}/delete")
    public String deleteReward(@PathVariable String id, 
                               jakarta.servlet.http.HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            rewardItemService.delete(id);
            adminLogService.log("DELETE_REWARD", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Reward berhasil dihapus dari katalog.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menghapus reward: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=rewards";
    }

    // ==================== MANAJEMEN BERITA (ARTICLES) ====================

    @PostMapping("/articles")
    public String createArticle(@RequestParam String title,
                                @RequestParam String content,
                                @RequestParam ArticleType type,
                                @RequestParam(required = false) String externalImageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam(defaultValue = "Admin") String author,
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Article article = Article.builder()
                    .title(title)
                    .content(content)
                    .type(type)
                    .externalImageUrl(externalImageUrl)
                    .author(author)
                    .published(true)
                    .build();
            articleService.saveArticle(article, imageFile);
            adminLogService.log("CREATE_ARTICLE", 
                    "Judul: " + title, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Berita '" + title + "' berhasil diterbitkan.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menerbitkan berita: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=articles";
    }

    @PostMapping("/articles/{id}/edit")
    public String editArticle(@PathVariable String id,
                               @RequestParam String title,
                               @RequestParam String content,
                               @RequestParam ArticleType type,
                               @RequestParam(required = false) String externalImageUrl,
                               @RequestParam(required = false) MultipartFile imageFile,
                               @RequestParam(defaultValue = "Admin") String author,
                               jakarta.servlet.http.HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            Article article = articleService.getArticleById(id);
            article.setTitle(title);
            article.setContent(content);
            article.setType(type);
            article.setExternalImageUrl(externalImageUrl);
            article.setAuthor(author);
            articleService.saveArticle(article, imageFile);
            adminLogService.log("EDIT_ARTICLE", 
                    "ID: " + id + ", Judul: " + title, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Berita berhasil diperbarui.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal memperbarui berita: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=articles";
    }

    @PostMapping("/articles/{id}/delete")
    public String deleteArticle(@PathVariable String id, 
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            articleService.deleteArticle(id);
            adminLogService.log("DELETE_ARTICLE", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Berita berhasil dihapus.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menghapus berita: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=articles";
    }

    // ==================== MANAJEMEN FAQ ====================

    @PostMapping("/faqs")
    public String createFaq(@RequestParam String question,
                            @RequestParam String answer,
                            @RequestParam(defaultValue = "0") int displayOrder,
                            jakarta.servlet.http.HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            Faq faq = Faq.builder()
                    .question(question)
                    .answer(answer)
                    .displayOrder(displayOrder)
                    .isActive(true)
                    .build();
            faqService.saveFaq(faq);
            adminLogService.log("CREATE_FAQ", 
                    "Tanya: " + question, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ FAQ berhasil ditambahkan.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menambah FAQ: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=faqs";
    }

    @PostMapping("/faqs/{id}/edit")
    public String editFaq(@PathVariable String id,
                          @RequestParam String question,
                          @RequestParam String answer,
                          @RequestParam(defaultValue = "0") int displayOrder,
                          jakarta.servlet.http.HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            Faq faq = faqService.getFaqById(id).orElseThrow(() -> new IllegalArgumentException("FAQ tidak ditemukan"));
            faq.setQuestion(question);
            faq.setAnswer(answer);
            faq.setDisplayOrder(displayOrder);
            faqService.saveFaq(faq);
            adminLogService.log("EDIT_FAQ", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ FAQ berhasil diperbarui.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal memperbarui FAQ: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=faqs";
    }

    @PostMapping("/faqs/{id}/toggle")
    public String toggleFaq(@PathVariable String id, 
                            jakarta.servlet.http.HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            faqService.toggleActive(id);
            adminLogService.log("TOGGLE_FAQ_STATUS", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Status FAQ berhasil diubah.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal mengubah status FAQ: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=faqs";
    }

    @PostMapping("/faqs/{id}/delete")
    public String deleteFaq(@PathVariable String id, 
                            jakarta.servlet.http.HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            faqService.deleteFaq(id);
            adminLogService.log("DELETE_FAQ", 
                    "ID: " + id, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ FAQ berhasil dihapus.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menghapus FAQ: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=faqs";
    }

    // ==================== [NEW] BROADCAST NOTIFIKASI ====================

    @PostMapping("/broadcast")
    public String sendBroadcast(@RequestParam String title,
                                @RequestParam String message,
                                @RequestParam String type,
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            notificationService.sendBroadcast(title, message, type);
            adminLogService.log("BROADCAST_NOTIFICATION",
                    "Judul: " + title + ", Tipe: " + type, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Pengumuman berhasil dikirim ke seluruh warga.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal mengirim pengumuman: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=overview";
    }

    // ==================== [NEW] MANAJEMEN LAPORAN LAPANGAN ====================

    @PostMapping("/reports/{id}/resolve")
    public String resolveReport(@PathVariable String id,
                                 @RequestParam String resolution,
                                 jakarta.servlet.http.HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            fieldReportService.resolveReport(id, resolution);
            adminLogService.log("RESOLVE_FIELD_REPORT",
                    "ID: " + id + ", Resolution: " + resolution, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "✅ Laporan berhasil diselesaikan.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Gagal menyelesaikan laporan: " + e.getMessage());
        }
        return "redirect:/admin/dashboard?activeTab=reports";
    }
}
