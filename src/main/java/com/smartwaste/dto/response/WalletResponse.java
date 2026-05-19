package com.smartwaste.dto.response;

/**
 * DTO response Green Wallet — saldo poin warga.
 */
public class WalletResponse {

    private String walletId;
    private String citizenId;
    private String citizenName;
    private double totalPoints;
    private double redeemedPoints;
    private double availablePoints;
    private Double targetPoints;

    public WalletResponse() {}

    public static WalletResponseBuilder builder() {
        return new WalletResponseBuilder();
    }

    public static class WalletResponseBuilder {
        private WalletResponse r = new WalletResponse();
        public WalletResponseBuilder walletId(String id) { r.walletId = id; return this; }
        public WalletResponseBuilder citizenId(String id) { r.citizenId = id; return this; }
        public WalletResponseBuilder citizenName(String name) { r.citizenName = name; return this; }
        public WalletResponseBuilder totalPoints(double p) { r.totalPoints = p; return this; }
        public WalletResponseBuilder redeemedPoints(double p) { r.redeemedPoints = p; return this; }
        public WalletResponseBuilder availablePoints(double p) { r.availablePoints = p; return this; }
        public WalletResponseBuilder targetPoints(Double p) { r.targetPoints = p; return this; }
        public WalletResponse build() { return r; }
    }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }
    public double getTotalPoints() { return totalPoints; }
    public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
    public double getRedeemedPoints() { return redeemedPoints; }
    public void setRedeemedPoints(double redeemedPoints) { this.redeemedPoints = redeemedPoints; }
    public double getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(double availablePoints) { this.availablePoints = availablePoints; }
    public Double getTargetPoints() { return targetPoints; }
    public void setTargetPoints(Double targetPoints) { this.targetPoints = targetPoints; }

    public int getProgressToTarget() {
        if (targetPoints == null || targetPoints <= 0) return 0;
        double progress = (totalPoints / targetPoints) * 100;
        return progress > 100 ? 100 : (int) progress;
    }

    /** Level gamifikasi berdasarkan total poin */
    public String getLevel() {
        if (totalPoints >= 10000) return "🏆 Platinum Eco Warrior";
        if (totalPoints >= 5000)  return "🥇 Gold Eco Hero";
        if (totalPoints >= 1000)  return "🥈 Silver Green Star";
        if (totalPoints >= 500)   return "🥉 Bronze Recycler";
        return "🌱 Green Starter";
    }

    /** Persentase menuju level berikutnya (untuk progress bar) */
    public int getProgressToNextLevel() {
        if (totalPoints >= 10000) return 100;
        if (totalPoints >= 5000)  return (int) ((totalPoints - 5000) / 50);
        if (totalPoints >= 1000)  return (int) ((totalPoints - 1000) / 40);
        if (totalPoints >= 500)   return (int) ((totalPoints - 500) / 5);
        return (int) (totalPoints / 5);
    }

    /** Ikon badge sesuai level saat ini */
    public String getBadgeIcon() {
        if (totalPoints >= 10000) return "🏆";
        if (totalPoints >= 5000)  return "🥇";
        if (totalPoints >= 1000)  return "🥈";
        if (totalPoints >= 500)   return "🥉";
        return "🌱";
    }

    /** Nama level berikutnya */
    public String getNextLevel() {
        if (totalPoints >= 10000) return "Level Tertinggi";
        if (totalPoints >= 5000)  return "Platinum Eco Warrior";
        if (totalPoints >= 1000)  return "Gold Eco Hero";
        if (totalPoints >= 500)   return "Silver Green Star";
        return "Bronze Recycler";
    }

    /** Sisa poin menuju level berikutnya */
    public long getPointsToNextLevel() {
        if (totalPoints >= 10000) return 0;
        if (totalPoints >= 5000)  return (long) (10000 - totalPoints);
        if (totalPoints >= 1000)  return (long) (5000  - totalPoints);
        if (totalPoints >= 500)   return (long) (1000  - totalPoints);
        return (long) (500 - totalPoints);
    }
}
