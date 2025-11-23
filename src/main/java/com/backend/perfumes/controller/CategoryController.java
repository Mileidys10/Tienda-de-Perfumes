package com.backend.perfumes.controller;

import com.backend.perfumes.model.Category;
import com.backend.perfumes.services.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> crearCategory(@RequestBody Category category) {
        try {
            Category nueva = categoryService.crearCategory(category);

            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("id", nueva.getId());
            responseData.put("name", nueva.getName());
            responseData.put("description", nueva.getDescription());
            responseData.put("imageUrl", nueva.getImageUrl() != null ? nueva.getImageUrl() : "");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Categoría creada exitosamente",
                    "data", responseData
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<Category>> listarCategories() {
        return ResponseEntity.ok(categoryService.listarCategories());
    }

    @GetMapping("/public")
    public ResponseEntity<?> listarCategoriesPublicas() {
        try {
            List<Category> categorias = categoryService.listarCategoriesPublicas();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", categorias,
                    "total", categorias.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.obtenerCategoryPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> actualizarCategory(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(categoryService.actualizarCategory(id, category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarCategory(@PathVariable Long id) {
        categoryService.eliminarCategory(id);
        return ResponseEntity.ok("Categoría eliminada correctamente.");
    }
}