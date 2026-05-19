package com.smartwaste.service;

import com.smartwaste.entity.Article;
import com.smartwaste.entity.enums.ArticleType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ArticleService {
    Article saveArticle(Article article, MultipartFile file);
    List<Article> getAllPublishedArticles();
    List<Article> getArticlesByType(ArticleType type);
    Article getArticleById(String id);
    void deleteArticle(String id);
    List<Article> getAllArticles(); // For Admin
}
