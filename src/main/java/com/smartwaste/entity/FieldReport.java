package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "field_reports")
public class FieldReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private Collector collector;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column
    private String photoUrl;

    @Column
    private String location;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, RESOLVED, REJECTED

    // Handle deleted collector gracefully for Thymeleaf
    public Collector getCollector() {
        try {
            if (this.collector != null) {
                // Force proxy initialization
                this.collector.getName();
            }
            return this.collector;
        } catch (jakarta.persistence.EntityNotFoundException e) {
            Collector dummy = new Collector();
            dummy.setName("Petugas Dihapus");
            return dummy;
        }
    }
}
