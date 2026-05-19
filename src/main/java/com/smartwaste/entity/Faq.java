package com.smartwaste.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq extends BaseEntity {

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "display_order")
    private Integer displayOrder;
}
