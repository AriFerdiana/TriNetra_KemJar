package com.smartwaste.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO untuk penukaran poin Green Wallet.
 */
public class RedeemPointsRequest {

    @NotNull(message = "Jumlah poin tidak boleh kosong")
    @DecimalMin(value = "10.0", message = "Minimum penukaran adalah 10 poin")
    private Double points;

    @NotBlank(message = "Deskripsi reward tidak boleh kosong")
    @Size(max = 500, message = "Deskripsi maksimal 500 karakter")
    private String description;

    private String rewardItemId;

    public RedeemPointsRequest() {}

    public Double getPoints() { return points; }
    public void setPoints(Double points) { this.points = points; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(String rewardItemId) { this.rewardItemId = rewardItemId; }
}
