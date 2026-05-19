package com.smartwaste.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "badges")
public class Badge extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String icon;

    /** Type of requirement, e.g., "TOTAL_DEPOSITS", "TOTAL_WEIGHT", "CATEGORY_PLASTIC" */
    @Column(nullable = false)
    private String requirementType;

    /** Threshold to achieve this badge */
    @Column(nullable = false)
    private Double threshold;
}
