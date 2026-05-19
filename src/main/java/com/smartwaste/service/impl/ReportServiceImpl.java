package com.smartwaste.service.impl;

import com.smartwaste.dto.response.ReportSummaryResponse;
import com.smartwaste.entity.GreenWallet;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.CollectorRepository;
import com.smartwaste.repository.GreenWalletRepository;
import com.smartwaste.repository.WasteDepositRepository;
import com.smartwaste.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.smartwaste.entity.enums.DepositStatus.*;

/**
 * Implementasi service laporan administratif.
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final CitizenRepository citizenRepository;
    private final CollectorRepository collectorRepository;
    private final WasteDepositRepository depositRepository;
    private final GreenWalletRepository walletRepository;

    public ReportServiceImpl(CitizenRepository citizenRepository,
                             CollectorRepository collectorRepository,
                             WasteDepositRepository depositRepository,
                             GreenWalletRepository walletRepository) {
        this.citizenRepository = citizenRepository;
        this.collectorRepository = collectorRepository;
        this.depositRepository = depositRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public ReportSummaryResponse getSummary() {
        // Hitung statistik utama
        long totalCitizens = citizenRepository.countByActiveTrue();
        
        // Warga aktif: minimal 1 setoran dalam 30 hari terakhir
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        long activeCitizens = depositRepository.countDistinctCitizensSince(thirtyDaysAgo);

        long totalCollectors = collectorRepository.countByActiveTrue();
        long totalDeposits = depositRepository.count();
        long pending = depositRepository.countByStatus(PENDING);
        long confirmed = depositRepository.countByStatus(CONFIRMED);
        long rejected = depositRepository.countByStatus(REJECTED);
        double totalWeight = depositRepository.sumTotalWeightConfirmed();
        double totalPoints = depositRepository.sumTotalPointsDistributed();

        // Statistik bulanan
        List<ReportSummaryResponse.MonthlyStatDto> monthlyStats = new ArrayList<>();
        List<Object[]> rawMonthly = depositRepository.findMonthlyStats();
        for (Object[] row : rawMonthly) {
            monthlyStats.add(ReportSummaryResponse.MonthlyStatDto.builder()
                    .monthYear((String) row[0])
                    .depositCount(((Number) row[1]).longValue())
                    .totalWeightKg(row[2] != null ? ((Number) row[2]).doubleValue() : 0)
                    .totalPoints(row[3] != null ? ((Number) row[3]).doubleValue() : 0)
                    .build());
        }

        // Top 5 citizen berdasarkan poin
        List<ReportSummaryResponse.CitizenLeaderboard> topCitizens = new ArrayList<>();
        citizenRepository.findTopByPoints(PageRequest.of(0, 5)).forEach(c -> {
            GreenWallet w = walletRepository.findByCitizen(c).orElse(null);
            double pts = w != null ? w.getTotalPoints() : 0;
            long deps = depositRepository.countByCitizenAndStatus(c, CONFIRMED);
            double weight = depositRepository.sumWeightByCitizen(c);
            String level = getEcoLevel(pts);
            
            topCitizens.add(ReportSummaryResponse.CitizenLeaderboard.builder()
                    .name(c.getName())
                    .totalPoints(pts)
                    .totalDeposits(deps)
                    .totalWeightKg(weight)
                    .level(level)
                    .badgeIcon(getBadgeForLevel(level))
                    .build());
        });

        // Breakdown per kategori
        java.util.Map<String, Double> weightByCategory = new java.util.HashMap<>();
        java.util.Map<String, Long> depositsByCategory = new java.util.HashMap<>();
        List<Object[]> categoryStats = depositRepository.findCategoryStats();
        for (Object[] row : categoryStats) {
            weightByCategory.put((String) row[0], ((Number) row[1]).doubleValue());
            depositsByCategory.put((String) row[0], ((Number) row[2]).longValue());
        }

        // Distribusi level warga berdasarkan total poin di wallet
        Map<String, Long> levelDistribution = new LinkedHashMap<>();
        levelDistribution.put("Platinum (≥10.000 pts)", 0L);
        levelDistribution.put("Gold (≥5.000 pts)", 0L);
        levelDistribution.put("Silver (≥1.000 pts)", 0L);
        levelDistribution.put("Bronze (≥500 pts)", 0L);
        levelDistribution.put("Starter (<500 pts)", 0L);
        walletRepository.findAll().forEach(w -> {
            double pts = w.getTotalPoints();
            if (pts >= 10000) levelDistribution.merge("Platinum (≥10.000 pts)", 1L, Long::sum);
            else if (pts >= 5000) levelDistribution.merge("Gold (≥5.000 pts)", 1L, Long::sum);
            else if (pts >= 1000) levelDistribution.merge("Silver (≥1.000 pts)", 1L, Long::sum);
            else if (pts >= 500)  levelDistribution.merge("Bronze (≥500 pts)", 1L, Long::sum);
            else                  levelDistribution.merge("Starter (<500 pts)", 1L, Long::sum);
        });

        return ReportSummaryResponse.builder()
                .totalCitizens(totalCitizens)
                .activeCitizensCount(activeCitizens)
                .totalCollectors(totalCollectors)
                .totalDeposits(totalDeposits)
                .pendingDeposits(pending)
                .confirmedDeposits(confirmed)
                .rejectedDeposits(rejected)
                .totalWeightKg(totalWeight)
                .totalPointsDistributed(totalPoints)
                .monthlyStats(monthlyStats)
                .topCitizens(topCitizens)
                .weightByCategory(weightByCategory)
                .depositsByCategory(depositsByCategory)
                .levelDistribution(levelDistribution)
                .build();
    }

    private String getEcoLevel(double pts) {
        if (pts >= 10000) return "Platinum Eco Warrior";
        if (pts >= 5000)  return "Gold Eco Hero";
        if (pts >= 1000)  return "Silver Green Star";
        if (pts >= 500)   return "Bronze Recycler";
        return "Green Starter";
    }

    private String getBadgeForLevel(String level) {
        if (level.contains("Platinum")) return "🛡️";
        if (level.contains("Gold")) return "🏆";
        if (level.contains("Silver")) return "💎";
        if (level.contains("Bronze")) return "🌟";
        return "🌱";
    }

    @Override
    public java.util.Map<String, Object> getCitizenEcoStats(String citizenEmail) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        com.smartwaste.entity.Citizen citizen = citizenRepository.findByEmail(citizenEmail).orElseThrow();
        
        // Deposits by Category
        List<Object[]> categoryStatsRaw = depositRepository.findCategoryStatsByCitizen(citizen);
        java.util.Map<String, Double> weightByCategory = new java.util.HashMap<>();
        for (Object[] row : categoryStatsRaw) {
            weightByCategory.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        
        // Monthly trend (Weight)
        List<Object[]> monthlyStatsRaw = depositRepository.findMonthlyStatsByCitizen(citizen);
        List<String> months = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Object[] row : monthlyStatsRaw) {
            months.add((String) row[0]);
            weights.add(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
        }
        
        stats.put("weightByCategory", weightByCategory);
        stats.put("trendMonths", months);
        stats.put("trendWeights", weights);
        
        return stats;
    }
}
