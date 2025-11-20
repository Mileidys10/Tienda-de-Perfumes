package com.backend.perfumes.controller;

import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Genre;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.services.PerfumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/perfumes")
@Slf4j
@Tag(name = "Perfumes", description = "Gestión de perfumes")
public class PerfumeController {

    private final PerfumeService perfumeService;

    public PerfumeController(PerfumeService perfumeService) {
        this.perfumeService = perfumeService;
    }

    @Operation(summary = "Obtener todos los perfumes", description = "Endpoint público para listar perfumes")
    @GetMapping
    public ResponseEntity<?> listarPerfumes(
            Pageable pageable,
            @RequestParam(value = "filtro", required = false) String filtro) {

        Page<Perfume> perfumes = perfumeService.listarPerfume(pageable, filtro);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", perfumes.getContent().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()),
                "meta", Map.of(
                        "total", perfumes.getTotalElements(),
                        "page", perfumes.getNumber(),
                        "size", perfumes.getSize(),
                        "totalPages", perfumes.getTotalPages()
                )
        ));
    }

    @PostMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> crearPerfume(
            @Valid @RequestBody PerfumeDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errores = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ?
                                    fieldError.getDefaultMessage() : "Error de validación"
                    ));

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", "Errores de validación",
                            "errors", errores,
                            "timestamp", LocalDateTime.now()
                    ));
        }

        try {
            Perfume perfume = perfumeService.savePerfume(dto, userDetails.getUsername());

            PerfumeDTO perfumeDTO = convertToDto(perfume);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Perfume creado exitosamente");
            response.put("data", perfumeDTO);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/mis-perfumes")
    @Operation(summary = "Listar perfumes del usuario autenticado")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> listarMisPerfumes(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "filtro", required = false) String filtro,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Perfume> perfumes = perfumeService.listarPerfumePorUsuario(
                    userDetails.getUsername(), pageable, filtro);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", perfumes.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            response.put("meta", Map.of(
                    "total", perfumes.getTotalElements(),
                    "page", perfumes.getNumber(),
                    "size", perfumes.getSize(),
                    "totalPages", perfumes.getTotalPages()
            ));
            response.put("user", userDetails.getUsername());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @Operation(summary = "Actualizar un perfume", description = "Solo disponible para el propietario del perfume o administradores")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> actualizarPerfume(
            @PathVariable Long id,
            @Valid @RequestBody PerfumeDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errores = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ?
                                    fieldError.getDefaultMessage() : "Error de validación"
                    ));

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", "Errores de validación",
                            "errors", errores
                    ));
        }

        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Perfume actualizado exitosamente",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @Operation(summary = "Eliminar un perfume", description = "Solo disponible para el propietario del perfume o administradores")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> eliminarPerfume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Perfume eliminado exitosamente",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    private PerfumeDTO convertToDto(Perfume perfume) {
        PerfumeDTO dto = new PerfumeDTO();
        dto.setId(perfume.getId());
        dto.setName(perfume.getName());
        dto.setDescription(perfume.getDescription());
        dto.setPrice(perfume.getPrice());
        dto.setStock(perfume.getStock());
        dto.setSizeMl(perfume.getSizeMl());
        dto.setGenre(Genre.valueOf(perfume.getGenre().name()));
        dto.setReleaseDate(perfume.getReleaseDate());
        dto.setBrandId(perfume.getBrand().getId());
        dto.setCategoryId(perfume.getCategory().getId());

        dto.setImageUrl(perfume.getImageUrl());

        System.out.println("🖼️ DEBUG - Perfume ID: " + perfume.getId() +
                ", Image URL from DB: " + perfume.getImageUrl());

        if (perfume.getUser() != null) {
            dto.setCreador(perfume.getUser().getUsername());
        }

        if (perfume.getBrand() != null) {
            dto.setBrandName(perfume.getBrand().getName());
        }
        if (perfume.getCategory() != null) {
            dto.setCategoryName(perfume.getCategory().getName());
        }

        return dto;
    }
}