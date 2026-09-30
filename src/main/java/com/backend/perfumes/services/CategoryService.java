package com.backend.perfumes.services;

import com.backend.perfumes.model.Category;
import com.backend.perfumes.model.ModerationStatus;
import com.backend.perfumes.repositories.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AutoModerationService autoModerationService;

    public CategoryService(CategoryRepository categoryRepository, AutoModerationService autoModerationService) {
        this.categoryRepository = categoryRepository;
        this.autoModerationService = autoModerationService;
    }
    private static final String EXISTING_CATEGORY_MSG = "A category with that name already exists";
    private  static final String NOT_FOUND_MSG = "Category with id %d not found";

    public Category crearCategory(Category category) {
        category.setId(null);

        if (category.getImageUrl() == null) {
            category.setImageUrl("");
        }

        if (category.getCreatedAt() == null) {
            category.setCreatedAt(LocalDateTime.now());
        }

        if (category.getUpdatedAt() == null) {
            category.setUpdatedAt(LocalDateTime.now());
        }

        category.setModerationStatus(ModerationStatus.APPROVED);
        category.setModeratedBy("SYSTEM");
        category.setModerationDate(LocalDateTime.now());

        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(EXISTING_CATEGORY_MSG);
        }
    }

    public List<Category> listarCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> listarCategoriesPublicas() {
        return categoryRepository.findByModerationStatus(ModerationStatus.APPROVED);
    }

    public Category obtenerCategoryPorId(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_MSG));
    }

    public void eliminarCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException(NOT_FOUND_MSG);
        }
        categoryRepository.deleteById(id);
    }

    public Category actualizarCategory(Long id, Category categoryActualizada) {
        Category categoriaExistente = obtenerCategoryPorId(id);
        categoriaExistente.setName(categoryActualizada.getName());
        categoriaExistente.setDescription(categoryActualizada.getDescription());
        categoriaExistente.setImageUrl(categoryActualizada.getImageUrl());
        return categoryRepository.save(categoriaExistente);
    }
}