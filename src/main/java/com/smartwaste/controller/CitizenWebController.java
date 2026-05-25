package com.smartwaste.controller;

import com.smartwaste.dto.request.CreateWasteDepositRequest;
import com.smartwaste.service.CitizenService;
import com.smartwaste.service.GreenWalletService;
import com.smartwaste.service.WasteDepositService;
import com.smartwaste.service.WasteCategoryService;
import com.smartwaste.service.ReportService;
import com.smartwaste.service.impl.FileStorageService;
import com.smartwaste.service.impl.PdfExportService;
import com.smartwaste.service.AchievementService;
import com.smartwaste.entity.WasteDeposit;
import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.PickupRequest;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.WasteDepositRepository;
import com.smartwaste.repository.PickupRequestRepository;
import com.smartwaste.service.CollectorService;
import com.smartwaste.service.ArticleService;
import com.smartwaste.service.FaqService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.smartwaste.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller untuk halaman dashboard Citizen.
 * Mengambil data dari service layer dan meneruskan ke Thymeleaf template.
 */
@Controller
@RequestMapping("/citizen")
@PreAuthorize("hasRole('CITIZEN')")
public class CitizenWebController {

    private final CitizenService citizenService;
    private final GreenWalletService walletService;
    private final WasteDepositService depositService;
    private final WasteCategoryService categoryService;
    private final FileStorageService fileStorageService;
    private final ReportService reportService;
    private final com.smartwaste.service.RewardItemService rewardItemService;
    private final PdfExportService pdfExportService;
    private final WasteDepositRepository wasteDepositRepository;
    private final CitizenRepository citizenRepository;
    private final AchievementService achievementService;
    private final CollectorService collectorService;
    private final PickupRequestRepository pickupRequestRepository;
    private final ArticleService articleService;
    private final FaqService faqService;
    private final com.smartwaste.service.SmartBinService smartBinService;
    private final NotificationService notificationService;

    public CitizenWebController(CitizenService citizenService,
                                GreenWalletService walletService,
                                WasteDepositService depositService,
                                WasteCategoryService categoryService,
                                FileStorageService fileStorageService,
                                ReportService reportService,
                                com.smartwaste.service.RewardItemService rewardItemService,
                                PdfExportService pdfExportService,
                                WasteDepositRepository wasteDepositRepository,
                                CitizenRepository citizenRepository,
                                AchievementService achievementService,
                                CollectorService collectorService,
                                PickupRequestRepository pickupRequestRepository,
                                ArticleService articleService,
                                FaqService faqService,
                                com.smartwaste.service.SmartBinService smartBinService,
                                NotificationService notificationService) {
        this.citizenService = citizenService;
        this.walletService = walletService;
        this.depositService = depositService;
        this.categoryService = categoryService;
        this.fileStorageService = fileStorageService;
        this.reportService = reportService;
        this.rewardItemService = rewardItemService;
        this.pdfExportService = pdfExportService;
        this.wasteDepositRepository = wasteDepositRepository;
        this.citizenRepository = citizenRepository;
        this.achievementService = achievementService;
        this.collectorService = collectorService;
        this.pickupRequestRepository = pickupRequestRepository;
        this.articleService = articleService;
        this.faqService = faqService;
        this.smartBinService = smartBinService;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        String email = auth.getName();

        try {
            java.util.List<com.smartwaste.entity.Notification> unreadNotifs = notificationService.getUnreadNotifications(email);
            model.addAttribute("unreadNotifications", unreadNotifs);
            model.addAttribute("unreadCount", unreadNotifs.size());
        } catch (Exception e) {
            model.addAttribute("unreadNotifications", java.util.List.of());
            model.addAttribute("unreadCount", 0);
        }

        try {
            model.addAttribute("profile",    citizenService.getMyProfile(email));
            model.addAttribute("wallet",     walletService.getMyWallet(email));
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Gagal memuat profil Anda: " + com.smartwaste.util.ExceptionUtils.getFriendlyMessage(e));
            return "redirect:/auth/login";
        }

        try {
            model.addAttribute("deposits",   depositService.getMyDeposits(email,
                    PageRequest.of(0, 10, Sort.by("createdAt").descending())));
        } catch (Exception e) {
            model.addAttribute("deposits", org.springframework.data.domain.Page.empty());
        }

        model.addAttribute("categories", categoryService.getAllActive());
        
        try {
            model.addAttribute("topCitizens", reportService.getSummary().getTopCitizens());
        } catch (Exception e) {
            model.addAttribute("topCitizens", java.util.List.of());
        }

        try {
            Citizen citizenProfile = citizenRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Citizen not found: " + email));
            model.addAttribute("ecoStats", reportService.getCitizenEcoStats(email));
            model.addAttribute("badges", achievementService.getCitizenBadges(citizenProfile));
        } catch (Exception e) {
            model.addAttribute("ecoStats", java.util.Map.of());
            model.addAttribute("badges", java.util.List.of());
        }

        model.addAttribute("pageTitle",  "Dashboard Warga");
        
        // --- NEW: Real-Time Daily Quests ---
        List<Map<String, Object>> quests = new ArrayList<>();
        
        // Quest 1: Setor 2kg Sampah Hari Ini
        Map<String, Object> q1 = new HashMap<>();
        q1.put("title", "Setor 2kg Sampah Hari Ini");
        q1.put("reward", 50);
        q1.put("icon", "⚖️");
        
        double todayWeight = 0.0;
        try {
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
            java.time.LocalDateTime endOfDay = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
            var deposits = depositService.getAllDeposits(null, startOfDay, endOfDay, null, PageRequest.of(0, 100));
            todayWeight = deposits.getContent().stream()
                .filter(d -> email.equals(d.getCitizenId()) || email.equals(auth.getName()))
                .mapToDouble(com.smartwaste.dto.response.WasteDepositResponse::getWeightKg)
                .sum();
        } catch (Exception e) {}
        
        int progress1 = (int) ((todayWeight / 2.0) * 100);
        q1.put("progress", progress1 > 100 ? 100 : progress1);
        quests.add(q1);
        
        // Quest 2: Tukar Hadiah Pertama
        Map<String, Object> q2 = new HashMap<>();
        q2.put("title", "Lakukan Penukaran Pertama");
        q2.put("reward", 100);
        q2.put("icon", "🎁");
        
        int progress2 = 0;
        try {
            var wallet = walletService.getMyWallet(email);
            if (wallet.getRedeemedPoints() > 0) {
                progress2 = 100;
            }
        } catch (Exception e) {}
        
        q2.put("progress", progress2);
        quests.add(q2);
        
        model.addAttribute("quests", quests);

        // Ambil katalog hadiah asli dari database
        List<com.smartwaste.entity.RewardItem> rewardItems = rewardItemService.getAllActive();
        List<Map<String, Object>> rewards = new ArrayList<>();
        
        for (com.smartwaste.entity.RewardItem item : rewardItems) {
            Map<String, Object> reward = new HashMap<>();
            reward.put("id", item.getId());
            reward.put("name", item.getName());
            reward.put("points", item.getPointsCost());
            reward.put("icon", item.getIcon());
            reward.put("redeemCount", 0); // TODO: Implement real redeem count if needed
            reward.put("isPopular", item.isPopular());
            reward.put("requiredLevel", item.getRequiredLevel());
            rewards.add(reward);
        }
        model.addAttribute("rewards", rewards);
        
        try {
            model.addAttribute("collectors", collectorService.getAllCollectors(PageRequest.of(0, 50)).getContent());
        } catch (Exception e) {
            model.addAttribute("collectors", java.util.List.of());
        }

        // Riwayat Penukaran
        try {
            model.addAttribute("redemptions", walletService.getMyRedemptions(email, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("createdAt").descending())).getContent());
        } catch (Exception e) {
            model.addAttribute("redemptions", java.util.List.of());
        }

        // Berita & Event (ambil 5 terbaru untuk Eco-Center)
        model.addAttribute("articles", articleService.getAllPublishedArticles());

        // FAQs
        model.addAttribute("faqs", faqService.getActiveFaqs());

        // Community Stats untuk Eco-Center
        try {
            Map<String, Object> commStats = new HashMap<>();
            commStats.put("totalWeightKg",   Math.round(wasteDepositRepository.sumTotalWeightConfirmed()));
            commStats.put("totalDeposits",   wasteDepositRepository.countByStatus(com.smartwaste.entity.enums.DepositStatus.CONFIRMED));
            commStats.put("totalCitizens",   citizenRepository.count());
            commStats.put("totalPointsDist", Math.round(wasteDepositRepository.sumTotalPointsDistributed()));
            model.addAttribute("commStats", commStats);
        } catch (Exception e) {
            model.addAttribute("commStats", java.util.Map.of(
                "totalWeightKg", 0L, "totalDeposits", 0L,
                "totalCitizens", 0L, "totalPointsDist", 0L
            ));
        }

        // Data Penjemputan (Pickup)
        try {
            Citizen citizen = citizenRepository.findByEmail(auth.getName()).orElse(null);
            if (citizen != null) {
                model.addAttribute("myPickups", pickupRequestRepository.findByCitizenOrderByPickupDateDesc(citizen, PageRequest.of(0, 5)).getContent());
                model.addAttribute("latestPickup", pickupRequestRepository.findTopByCitizenOrderByCreatedAtDesc(citizen).orElse(null));
                
                // Ambil tanggal pickup untuk kalender minggu ini
                java.time.LocalDate startOfWeek = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
                java.time.LocalDate endOfWeek = startOfWeek.plusDays(6);
                model.addAttribute("pickupDates", pickupRequestRepository.findPickupDatesByCitizenAndDateRange(citizen, startOfWeek, endOfWeek));
            }
        } catch (Exception e) {
            model.addAttribute("myPickups", java.util.List.of());
            model.addAttribute("latestPickup", null);
            model.addAttribute("pickupDates", java.util.List.of());
        }

        // --- NEW: Smart Bins for Monitoring ---
        model.addAttribute("smartBins", smartBinService.getAllBins());

        return "citizen/dashboard";
    }

    @PostMapping("/pickup/request")
    public String requestPickup(Authentication auth,
                                @RequestParam String pickupDate,
                                @RequestParam String shift,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            Citizen citizen = citizenRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Warga tidak ditemukan"));
            
            PickupRequest pickupRequest = new PickupRequest(
                    citizen,
                    java.time.LocalDate.parse(pickupDate),
                    shift,
                    notes
            );
            pickupRequestRepository.save(pickupRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Permintaan penjemputan berhasil diajukan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal mengajukan penjemputan: " + com.smartwaste.util.ExceptionUtils.getFriendlyMessage(e));
        }
        return "redirect:/citizen/dashboard";
    }

    @PostMapping("/deposit")
    public String createDeposit(Authentication auth, 
                                @RequestParam String categoryId, 
                                @RequestParam Double weightKg, 
                                @RequestParam(required = false) String notes,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        try {
            CreateWasteDepositRequest req = new CreateWasteDepositRequest();
            req.setCategoryId(categoryId);
            req.setWeightKg(weightKg);
            req.setNotes(notes);
            
            if (file != null && !file.isEmpty()) {
                String fileUrl = fileStorageService.storeFile(file);
                req.setImageUrl(fileUrl);
            }
            
            depositService.createDeposit(auth.getName(), req);
            redirectAttributes.addFlashAttribute("successMessage", "Berhasil membuat setoran! Harap tunggu petugas mengkonfirmasi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal menyetor sampah: " + com.smartwaste.util.ExceptionUtils.getFriendlyMessage(e));
        }
        return "redirect:/citizen/dashboard";
    }

    @PostMapping("/redeem")
    public String redeemPoints(Authentication auth, 
                               @RequestParam Double points, 
                               @RequestParam String description,
                               @RequestParam(required = false) String rewardItemId,
                               RedirectAttributes redirectAttributes) {
        try {
            walletService.requestRedemption(auth.getName(), points, description, rewardItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Permintaan penukaran poin Anda berhasil diajukan dan menunggu persetujuan admin.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal menukar poin: " + com.smartwaste.util.ExceptionUtils.getFriendlyMessage(e));
        }
        return "redirect:/citizen/dashboard";
    }

    @PostMapping("/settings")
    public String updateSettings(Authentication auth, 
                                 @RequestParam String name, 
                                 @RequestParam String phone, 
                                 @RequestParam String address,
                                 @RequestParam(required = false) Double targetPoints,
                                 RedirectAttributes redirectAttributes) {
        try {
            var profile = citizenService.getMyProfile(auth.getName());
            citizenService.updateProfile(profile.getId(), name, phone, address);
            if (targetPoints != null) {
                walletService.updateTargetPoints(auth.getName(), targetPoints);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil diperbarui!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gagal memperbarui profil: " + com.smartwaste.util.ExceptionUtils.getFriendlyMessage(e));
        }
        return "redirect:/citizen/dashboard";
    }

    @GetMapping("/deposit/{id}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@org.springframework.web.bind.annotation.PathVariable String id, Authentication auth) {
        try {
            WasteDeposit deposit = wasteDepositRepository.findById(id).orElseThrow(() -> new RuntimeException("Setoran tidak ditemukan"));
            
            // Verifikasi kepemilikan
            if (!deposit.getCitizen().getEmail().equals(auth.getName())) {
                throw new RuntimeException("Akses ditolak");
            }

            if (deposit.getStatus() != com.smartwaste.entity.enums.DepositStatus.CONFIRMED) {
                throw new RuntimeException("Hanya setoran yang sudah dikonfirmasi yang memiliki receipt");
            }

            byte[] pdfBytes = pdfExportService.exportGreenReceipt(deposit);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "GreenReceipt_" + deposit.getId() + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(("Error: " + com.smartwaste.util.ExceptionUtils.getFriendlyMessage(e)).getBytes());
        }
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(Authentication auth) {
        notificationService.markAllAsRead(auth.getName());
        return "redirect:/citizen/dashboard";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@org.springframework.web.bind.annotation.PathVariable String id, Authentication auth) {
        notificationService.markAsRead(id);
        return "redirect:/citizen/dashboard";
    }
}
