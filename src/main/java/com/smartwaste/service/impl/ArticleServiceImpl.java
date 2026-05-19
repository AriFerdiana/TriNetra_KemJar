package com.smartwaste.service.impl;

import com.smartwaste.entity.Article;
import com.smartwaste.entity.enums.ArticleType;
import com.smartwaste.repository.ArticleRepository;
import com.smartwaste.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Article saveArticle(Article article, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            String imagePath = fileStorageService.storeFile(file);
            article.setLocalImagePath(imagePath);
        }
        return articleRepository.save(article);
    }

    @Override
    public List<Article> getAllPublishedArticles() {
        return articleRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<Article> getArticlesByType(ArticleType type) {
        return articleRepository.findByTypeAndPublishedTrueOrderByCreatedAtDesc(type);
    }

    @Override
    public Article getArticleById(String id) {
        return articleRepository.findById(id).orElseThrow(() -> new RuntimeException("Article not found"));
    }

    @Override
    public void deleteArticle(String id) {
        articleRepository.deleteById(id);
    }

    @Override
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
}
