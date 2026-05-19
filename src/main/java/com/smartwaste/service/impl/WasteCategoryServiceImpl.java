package com.smartwaste.service.impl;

import com.smartwaste.dto.response.WasteCategoryResponse;
import com.smartwaste.entity.WasteCategory;
import com.smartwaste.entity.enums.WasteType;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.WasteCategoryRepository;
import com.smartwaste.service.WasteCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WasteCategoryServiceImpl implements WasteCategoryService {

    private final WasteCategoryRepository categoryRepository;

    public WasteCategoryServiceImpl(WasteCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<WasteCategoryResponse> getAllActive() {
        return categoryRepository.findByActiveTrue().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<WasteCategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public WasteCategoryResponse getById(String id) {
        return categoryRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", id));
    }

    @Override
    @Transactional
    public WasteCategoryResponse create(String name, String description, String type,
                                        double pointsPerKg, String iconUrl) {
        WasteCategory category = new WasteCategory(
                name, description, WasteType.valueOf(type.toUpperCase()), pointsPerKg, iconUrl);
        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public WasteCategoryResponse update(String id, String name, String description, double pointsPerKg) {
        WasteCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", id));
        if (name != null) cat.setName(name);
        if (description != null) cat.setDescription(description);
        if (pointsPerKg > 0) cat.setPointsPerKg(pointsPerKg);
        return mapToResponse(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        WasteCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", id));
        cat.setActive(!cat.isActive());
        categoryRepository.save(cat);
    }

    @Override
    @Transactional
    public void delete(String id) {
        WasteCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WasteCategory", "id", id));
        if (!cat.getDeposits().isEmpty()) {
            throw new IllegalStateException("Kategori tidak dapat dihapus karena sudah memiliki riwayat transaksi setoran. Silakan nonaktifkan kategori ini sebagai gantinya.");
        }
        categoryRepository.delete(cat);
    }

    private WasteCategoryResponse mapToResponse(WasteCategory cat) {
        String typeLabel = switch (cat.getType()) {
            case ORGANIC   -> "Organik";
            case INORGANIC -> "Anorganik";
            case B3        -> "B3 (Berbahaya)";
        };
        return WasteCategoryResponse.builder()
                .id(cat.getId()).name(cat.getName()).description(cat.getDescription())
                .type(cat.getType()).typeLabel(typeLabel).pointsPerKg(cat.getPointsPerKg())
                .iconUrl(cat.getIconUrl()).active(cat.isActive())
                .totalDeposits(cat.getDeposits().size())
                .build();
    }
}
