package com.smartwaste.dto.response;

import java.util.List;
import java.util.Map;

/**
 * DTO response laporan ringkasan sistem.
 */
public class ReportSummaryResponse {

    private long totalCitizens;
    private long activeCitizensCount;
    private long totalCollectors;
    private long totalDeposits;
    private long pendingDeposits;
    private long confirmedDeposits;
    private double totalWeightKg;
    private double totalPointsDistributed;
    private Map<String, Double> weightByCategory;
    private Map<String, Long> depositsByCategory;
    private Map<String, Long> levelDistribution;
    private long rejectedDeposits;
    private List<MonthlyStatDto> monthlyStats;
    private List<CitizenLeaderboard> topCitizens;

    public ReportSummaryResponse() {}

    public static ReportSummaryResponseBuilder builder() {
        return new ReportSummaryResponseBuilder();
    }

    public static class ReportSummaryResponseBuilder {
        private ReportSummaryResponse r = new ReportSummaryResponse();
        public ReportSummaryResponseBuilder totalCitizens(long c) { r.totalCitizens = c; return this; }
        public ReportSummaryResponseBuilder activeCitizensCount(long c) { r.activeCitizensCount = c; return this; }
        public ReportSummaryResponseBuilder totalCollectors(long c) { r.totalCollectors = c; return this; }
        public ReportSummaryResponseBuilder totalDeposits(long c) { r.totalDeposits = c; return this; }
        public ReportSummaryResponseBuilder pendingDeposits(long c) { r.pendingDeposits = c; return this; }
        public ReportSummaryResponseBuilder confirmedDeposits(long c) { r.confirmedDeposits = c; return this; }
        public ReportSummaryResponseBuilder totalWeightKg(double w) { r.totalWeightKg = w; return this; }
        public ReportSummaryResponseBuilder totalPointsDistributed(double p) { r.totalPointsDistributed = p; return this; }
        public ReportSummaryResponseBuilder weightByCategory(Map<String, Double> m) { r.weightByCategory = m; return this; }
        public ReportSummaryResponseBuilder depositsByCategory(Map<String, Long> m) { r.depositsByCategory = m; return this; }
        public ReportSummaryResponseBuilder levelDistribution(Map<String, Long> m) { r.levelDistribution = m; return this; }
        public ReportSummaryResponseBuilder rejectedDeposits(long c) { r.rejectedDeposits = c; return this; }
        public ReportSummaryResponseBuilder monthlyStats(List<MonthlyStatDto> l) { r.monthlyStats = l; return this; }
        public ReportSummaryResponseBuilder topCitizens(List<CitizenLeaderboard> l) { r.topCitizens = l; return this; }
        public ReportSummaryResponse build() { return r; }
    }

    public long getTotalCitizens() { return totalCitizens; }
    public void setTotalCitizens(long totalCitizens) { this.totalCitizens = totalCitizens; }
    public long getActiveCitizensCount() { return activeCitizensCount; }
    public void setActiveCitizensCount(long activeCitizensCount) { this.activeCitizensCount = activeCitizensCount; }
    public long getTotalCollectors() { return totalCollectors; }
    public void setTotalCollectors(long totalCollectors) { this.totalCollectors = totalCollectors; }
    public long getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(long totalDeposits) { this.totalDeposits = totalDeposits; }
    public long getPendingDeposits() { return pendingDeposits; }
    public void setPendingDeposits(long pendingDeposits) { this.pendingDeposits = pendingDeposits; }
    public long getConfirmedDeposits() { return confirmedDeposits; }
    public void setConfirmedDeposits(long confirmedDeposits) { this.confirmedDeposits = confirmedDeposits; }
    public double getTotalWeightKg() { return totalWeightKg; }
    public void setTotalWeightKg(double totalWeightKg) { this.totalWeightKg = totalWeightKg; }
    public double getTotalPointsDistributed() { return totalPointsDistributed; }
    public void setTotalPointsDistributed(double totalPointsDistributed) { this.totalPointsDistributed = totalPointsDistributed; }
    public Map<String, Double> getWeightByCategory() { return weightByCategory; }
    public void setWeightByCategory(Map<String, Double> weightByCategory) { this.weightByCategory = weightByCategory; }
    public Map<String, Long> getDepositsByCategory() { return depositsByCategory; }
    public void setDepositsByCategory(Map<String, Long> depositsByCategory) { this.depositsByCategory = depositsByCategory; }
    public Map<String, Long> getLevelDistribution() { return levelDistribution; }
    public void setLevelDistribution(Map<String, Long> levelDistribution) { this.levelDistribution = levelDistribution; }
    public long getRejectedDeposits() { return rejectedDeposits; }
    public void setRejectedDeposits(long rejectedDeposits) { this.rejectedDeposits = rejectedDeposits; }
    public List<MonthlyStatDto> getMonthlyStats() { return monthlyStats; }
    public void setMonthlyStats(List<MonthlyStatDto> monthlyStats) { this.monthlyStats = monthlyStats; }
    public List<CitizenLeaderboard> getTopCitizens() { return topCitizens; }
    public void setTopCitizens(List<CitizenLeaderboard> topCitizens) { this.topCitizens = topCitizens; }

    public static class MonthlyStatDto {
        private String monthYear;
        private long depositCount;
        private double totalWeightKg;
        private double totalPoints;

        public MonthlyStatDto() {}
        public static MonthlyStatDtoBuilder builder() { return new MonthlyStatDtoBuilder(); }
        public static class MonthlyStatDtoBuilder {
            private MonthlyStatDto s = new MonthlyStatDto();
            public MonthlyStatDtoBuilder monthYear(String s) { this.s.monthYear = s; return this; }
            public MonthlyStatDtoBuilder depositCount(long c) { this.s.depositCount = c; return this; }
            public MonthlyStatDtoBuilder totalWeightKg(double w) { this.s.totalWeightKg = w; return this; }
            public MonthlyStatDtoBuilder totalPoints(double p) { this.s.totalPoints = p; return this; }
            public MonthlyStatDto build() { return s; }
        }
        public String getMonthYear() { return monthYear; }
        public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
        public long getDepositCount() { return depositCount; }
        public void setDepositCount(long depositCount) { this.depositCount = depositCount; }
        public double getTotalWeightKg() { return totalWeightKg; }
        public void setTotalWeightKg(double totalWeightKg) { this.totalWeightKg = totalWeightKg; }
        public double getTotalPoints() { return totalPoints; }
        public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
    }

    public static class CitizenLeaderboard {
        private String name;
        private double totalPoints;
        private long totalDeposits;
        private double totalWeightKg;
        private String level;
        private String badgeIcon;

        public CitizenLeaderboard() {}
        public static CitizenLeaderboardBuilder builder() { return new CitizenLeaderboardBuilder(); }
        public static class CitizenLeaderboardBuilder {
            private CitizenLeaderboard l = new CitizenLeaderboard();
            public CitizenLeaderboardBuilder name(String n) { l.name = n; return this; }
            public CitizenLeaderboardBuilder totalPoints(double p) { l.totalPoints = p; return this; }
            public CitizenLeaderboardBuilder totalDeposits(long d) { l.totalDeposits = d; return this; }
            public CitizenLeaderboardBuilder totalWeightKg(double w) { l.totalWeightKg = w; return this; }
            public CitizenLeaderboardBuilder level(String lv) { l.level = lv; return this; }
            public CitizenLeaderboardBuilder badgeIcon(String icon) { l.badgeIcon = icon; return this; }
            public CitizenLeaderboard build() { return l; }
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getTotalPoints() { return totalPoints; }
        public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
        public long getTotalDeposits() { return totalDeposits; }
        public void setTotalDeposits(long totalDeposits) { this.totalDeposits = totalDeposits; }
        public double getTotalWeightKg() { return totalWeightKg; }
        public void setTotalWeightKg(double totalWeightKg) { this.totalWeightKg = totalWeightKg; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getBadgeIcon() { return badgeIcon; }
        public void setBadgeIcon(String badgeIcon) { this.badgeIcon = badgeIcon; }
    }
}
