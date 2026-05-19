package com.smartwaste.entity;

import com.smartwaste.entity.enums.ArticleType;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleType type;

    @Column(name = "external_image_url", columnDefinition = "TEXT")
    private String externalImageUrl;
    
    @Column(name = "local_image_path", columnDefinition = "TEXT")
    private String localImagePath;

    @Column(nullable = false)
    private String author;

    @Builder.Default
    private boolean published = true;

    public String getSummary() {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 97) + "..." : content;
    }

    public String getImageUrl() {
        if (localImagePath != null && !localImagePath.isEmpty()) {
            return localImagePath;
        }
        if (externalImageUrl != null && !externalImageUrl.isEmpty()) {
            return externalImageUrl;
        }
        return "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800";
    }
}
