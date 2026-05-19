package com.smartwaste.service;

import com.smartwaste.dto.response.WasteCategoryResponse;

import java.util.List;

/**
 * Service interface untuk manajemen kategori sampah.
 */
public interface WasteCategoryService {
    List<WasteCategoryResponse> getAllActive();
    List<WasteCategoryResponse> getAll();
    WasteCategoryResponse getById(String id);
    WasteCategoryResponse create(String name, String description, String type, double pointsPerKg, String iconUrl);
    WasteCategoryResponse update(String id, String name, String description, double pointsPerKg);
    void toggleActive(String id);
    void delete(String id);
}
