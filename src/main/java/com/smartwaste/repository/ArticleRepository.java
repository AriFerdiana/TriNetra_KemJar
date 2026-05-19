package com.smartwaste.repository;

import com.smartwaste.entity.Article;
import com.smartwaste.entity.enums.ArticleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String> {
    List<Article> findByPublishedTrueOrderByCreatedAtDesc();
    List<Article> findByTypeAndPublishedTrueOrderByCreatedAtDesc(ArticleType type);
}
