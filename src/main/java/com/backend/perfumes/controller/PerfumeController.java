package com.backend.perfumes.controller;

import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Genre;
import com.backend.perfumes.model.ModerationStatus;
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
import java.util.List;
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


    @Operation(summary = "Obtener todos los perfumes aprobados", description = "Endpoint público para listar perfumes")
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

    @Operation(summary = "Obtener perfume público por ID")
    @GetMapping("/public/{id}")
    public ResponseEntity<?> obtenerPerfumePublico(@PathVariable Long id) {
        try {
            Perfume perfume = perfumeService.obtenerPerfumePublico(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", convertToDto(perfume)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
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
            response.put("moderation", Map.of(
                    "status", perfume.getModerationStatus(),
                    "message", perfume.getModerationStatus() == ModerationStatus.APPROVED ?
                            "Aprobado automáticamente" :
                            perfume.getRejectionReason() != null ? perfume.getRejectionReason() : "En revisión"
            ));
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
            @RequestParam(value = "status", required = false) ModerationStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Perfume> perfumes;

            if (status != null) {
                perfumes = perfumeService.listarPerfumesPorEstado(status, pageable);
                // Filtrar adicionalmente por usuario
                perfumes = (Page<Perfume>) perfumes.filter(p -> p.getUser().getUsername().equals(userDetails.getUsername()));
            } else {
                perfumes = perfumeService.listarPerfumePorUsuario(userDetails.getUsername(), pageable, filtro);
            }

            // Estadísticas de moderación
            long totalAprobados = perfumes.getContent().stream().filter(p -> p.getModerationStatus() == ModerationStatus.APPROVED).count();
            long totalPendientes = perfumes.getContent().stream().filter(p -> p.getModerationStatus() == ModerationStatus.PENDING_REVIEW).count();
            long totalRechazados = perfumes.getContent().stream().filter(p -> p.getModerationStatus() == ModerationStatus.REJECTED).count();

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
            response.put("moderationStats", Map.of(
                    "approved", totalAprobados,
                    "pending", totalPendientes,
                    "rejected", totalRechazados
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


    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarTodosPerfumesAdmin(
            Pageable pageable,
            @RequestParam(value = "filtro", required = false) String filtro) {

        Page<Perfume> perfumes = perfumeService.listarPerfumesParaAdmin(pageable, filtro);

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

    @GetMapping("/admin/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarPerfumesPendientes() {
        try {
            List<Perfume> perfumes = perfumeService.obtenerPerfumesPendientes();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", perfumes.stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList()),
                    "total", perfumes.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprobarPerfume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Perfume perfume = perfumeService.aprobarPerfume(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Perfume aprobado exitosamente",
                    "data", convertToDto(perfume)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rechazarPerfume(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String motivo = request.get("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Se requiere un motivo para rechazar"
                ));
            }

            Perfume perfume = perfumeService.rechazarPerfume(id, motivo, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Perfume rechazado exitosamente",
                    "data", convertToDto(perfume)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
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
        dto.setModerationStatus(perfume.getModerationStatus());
        dto.setRejectionReason(perfume.getRejectionReason());

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
            Perfume perfumeActualizado = perfumeService.actualizarPerfume(id, dto, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Perfume actualizado exitosamente",
                    "data", convertToDto(perfumeActualizado),
                    "moderation", Map.of(
                            "status", perfumeActualizado.getModerationStatus(),
                            "message", perfumeActualizado.getModerationStatus() == ModerationStatus.APPROVED ?
                                    "Aprobado automáticamente" :
                                    perfumeActualizado.getRejectionReason() != null ? perfumeActualizado.getRejectionReason() : "En revisión"
                    ),
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> eliminarPerfume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            perfumeService.eliminarPerfume(id, userDetails.getUsername());

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
}