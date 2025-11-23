// FavoritesController.java
package com.backend.perfumes.controller;

import com.backend.perfumes.services.FavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Favorites", description = "Gestión de perfumes favoritos")
public class FavoritesController {

    private final FavoritesService favoritesService;

    @PostMapping("/{perfumeId}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Agregar perfume a favoritos")
    public ResponseEntity<?> addToFavorites(
            @PathVariable Long perfumeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            boolean success = favoritesService.addToFavorites(perfumeId, userDetails.getUsername());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Perfume agregado a favoritos");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @DeleteMapping("/{perfumeId}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Remover perfume de favoritos")
    public ResponseEntity<?> removeFromFavorites(
            @PathVariable Long perfumeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            boolean success = favoritesService.removeFromFavorites(perfumeId, userDetails.getUsername());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Perfume removido de favoritos");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Obtener perfumes favoritos del usuario")
    public ResponseEntity<?> getFavorites(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Object> favorites = favoritesService.getUserFavorites(userDetails.getUsername(), pageable)
                    .map(perfume -> {
                        // Convertir perfume a DTO
                        Map<String, Object> perfumeMap = new LinkedHashMap<>();
                        perfumeMap.put("id", perfume.getId());
                        perfumeMap.put("name", perfume.getName());
                        perfumeMap.put("price", perfume.getPrice());
                        perfumeMap.put("imageUrl", perfume.getImageUrl());
                        perfumeMap.put("brand", perfume.getBrand().getName());
                        return perfumeMap;
                    });

            long totalFavorites = favoritesService.getFavoriteCount(userDetails.getUsername());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", favorites.getContent());
            response.put("meta", Map.of(
                    "total", favorites.getTotalElements(),
                    "page", favorites.getNumber(),
                    "size", favorites.getSize(),
                    "totalPages", favorites.getTotalPages(),
                    "totalFavorites", totalFavorites
            ));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/{perfumeId}/is-favorite")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Verificar si un perfume está en favoritos")
    public ResponseEntity<?> isFavorite(
            @PathVariable Long perfumeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            boolean isFavorite = favoritesService.isFavorite(perfumeId, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "isFavorite", isFavorite,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}