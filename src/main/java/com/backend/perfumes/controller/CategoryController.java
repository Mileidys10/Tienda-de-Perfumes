package com.backend.perfumes.controller;

import com.backend.perfumes.model.Category;
import com.backend.perfumes.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categorias", description = "Gestion de categorias de perfumes")
public class CategoryController {

    private final CategoryService categoryService;

    private static final String CATEGORY_CREATED_MSG = "Categoria creada exitosamente";
    private static final String CATEGORY_DELETED_MSG = "Categoria eliminada correctamente.";

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Crear nueva categoria")
    public ResponseEntity<?> crearCategory(@RequestBody Category category) {
        try {
            log.info("Creando categoria: {}", category.getName());
            Category nueva = categoryService.crearCategory(category);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", nueva.getId());
            responseData.put("name", nueva.getName());
            responseData.put("description", nueva.getDescription());
            responseData.put("imageUrl", nueva.getImageUrl() != null ? nueva.getImageUrl() : "");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", CATEGORY_CREATED_MSG,
                    "data", responseData
            ));
        } catch (Exception e) {
            log.error("Error al crear categoria: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    @Operation(summary = "Listar todas las categorias")
    public ResponseEntity<List<Category>> listarCategories() {
        return ResponseEntity.ok(categoryService.listarCategories());
    }

    @GetMapping("/public")
    @Operation(summary = "Listar categorias publicas")
    public ResponseEntity<?> listarCategoriesPublicas() {
        try {
            List<Category> categorias = categoryService.listarCategoriesPublicas();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", categorias,
                    "total", categorias.size()
            ));
        } catch (Exception e) {
            log.error("Error al listar categorias publicas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoria por ID")
    public ResponseEntity<Category> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.obtenerCategoryPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Actualizar categoria existente")
    public ResponseEntity<Category> actualizarCategory(@PathVariable Long id, @RequestBody Category category) {
        log.info("Actualizando categoria ID: {}", id);
        return ResponseEntity.ok(categoryService.actualizarCategory(id, category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Eliminar categoria por ID")
    public ResponseEntity<String> eliminarCategory(@PathVariable Long id) {
        log.info("Eliminando categoria ID: {}", id);
        categoryService.eliminarCategory(id);
        return ResponseEntity.ok(CATEGORY_DELETED_MSG);
    }
}
