package com.backend.perfumes.services;

import com.backend.perfumes.model.Category;
import com.backend.perfumes.repositories.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category crearCategory(Category category) {
        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
    }

    public List<Category> listarCategories() {
        return categoryRepository.findAll();
    }

    public Category obtenerCategoryPorId(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));
    }

    public void eliminarCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("No existe la categoría con ID: " + id);
        }
        categoryRepository.deleteById(id);
    }

    public Category actualizarCategory(Long id, Category categoryActualizada) {
        Category categoriaExistente = obtenerCategoryPorId(id);
        categoriaExistente.setName(categoryActualizada.getName());
        return categoryRepository.save(categoriaExistente);
    }
}
