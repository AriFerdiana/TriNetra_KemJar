package com.smartwaste.entity;

import com.smartwaste.entity.enums.PickupStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pickup_requests")
public class PickupRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id")
    private Collector collector;

    @Column(name = "pickup_date", nullable = false)
    private LocalDate pickupDate;

    // 'MORNING' atau 'AFTERNOON'
    @Column(name = "shift", nullable = false, length = 20)
    private String shift;

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PickupStatus status = PickupStatus.PENDING;

    public PickupRequest(Citizen citizen, LocalDate pickupDate, String shift, String notes) {
        this.citizen = citizen;
        this.pickupDate = pickupDate;
        this.shift = shift;
        this.notes = notes;
    }
}
