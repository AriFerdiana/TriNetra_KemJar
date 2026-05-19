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
}
