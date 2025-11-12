package com.backend.perfumes.controller;

import com.backend.perfumes.model.Category;
import com.backend.perfumes.services.CategoryService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> crearCategory(@RequestBody Category category) {
        try {
            Category nueva = categoryService.crearCategory(category);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Categoría creada exitosamente",
                    "data", Map.of(
                            "id", nueva.getId(),
                            "name", nueva.getName(),
                            "description", nueva.getDescription()
                    )
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
