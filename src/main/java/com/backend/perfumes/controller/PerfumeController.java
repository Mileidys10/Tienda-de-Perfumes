package com.backend.perfumes.controller;

import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Genre;
import com.backend.perfumes.model.ModerationStatus;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.services.PerfumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Perfumes", description = "Gestion de perfumes")
public class PerfumeController {

    private final PerfumeService perfumeService;

    private static final String VALIDATIONS_ERRORS_MSG = "Errores de validacion";
    private static final String PERFUME_CREATED_MSG = "Perfume creado exitosamente";
    private static final String DELETED_PERFUME_MSG = "Perfume eliminado exitosamente";
    private static final String UPDATED_PERFUME_MSG = "Perfume actualizado exitosamente";
    private static final String APPROVED_MSG = "Perfume aprobado exitosamente";
    private static final String AUTOMATIC_APPROVED_MSG = "Aprobado automaticamente";
    private static final String PERFUME_UNDER_REVIEW_MSG = "Perfume en revision";
    private static final String PERFUME_DENIED_MSG = "Perfume rechazado exitosamente";
    private static final String REASON_REQUIRED_MSG = "Se requiere un motivo para rechazar";

    @Operation(summary = "Obtener todos los perfumes aprobados", description = "Endpoint publico para listar perfumes")
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

    @Operation(summary = "Obtener perfume publico por ID")
    @GetMapping("/public/{id}")
    public ResponseEntity<?> obtenerPerfumePublico(@PathVariable Long id) {
        try {
            Perfume perfume = perfumeService.obtenerPerfumePublico(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", convertToDto(perfume)
            ));
        } catch (Exception e) {
            log.error("Error obteniendo perfume publico ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Crear nuevo perfume")
    public ResponseEntity<?> crearPerfume(
            @Valid @RequestBody PerfumeDTO dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errores = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ?
                                    fieldError.getDefaultMessage() : VALIDATIONS_ERRORS_MSG
                    ));

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", VALIDATIONS_ERRORS_MSG,
                            "errors", errores,
                            "timestamp", LocalDateTime.now()
                    ));
        }

        try {
            log.info("Creando perfume '{}' para vendedor: {}", dto.getName(), userDetails.getUsername());
            Perfume perfume = perfumeService.savePerfume(dto, userDetails.getUsername());

            PerfumeDTO perfumeDTO = convertToDto(perfume);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", PERFUME_CREATED_MSG);
            response.put("data", perfumeDTO);
            response.put("moderation", Map.of(
                    "status", perfume.getModerationStatus(),
                    "message", perfume.getModerationStatus() == ModerationStatus.APPROVED ?
                            AUTOMATIC_APPROVED_MSG :
                            perfume.getRejectionReason() != null ? perfume.getRejectionReason() : PERFUME_DENIED_MSG
            ));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Error creando perfume: {}", e.getMessage(), e);
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
                perfumes = (Page<Perfume>) perfumes.filter(p -> p.getUser().getUsername().equals(userDetails.getUsername()));
            } else {
                perfumes = perfumeService.listarPerfumePorUsuario(userDetails.getUsername(), pageable, filtro);
            }

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
            log.error("Error listando mis perfumes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los perfumes para admin")
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
    @Operation(summary = "Listar perfumes pendientes de aprobacion")
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
            log.error("Error listando perfumes pendientes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar un perfume")
    public ResponseEntity<?> aprobarPerfume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Aprobando perfume ID {} por admin: {}", id, userDetails.getUsername());
            Perfume perfume = perfumeService.aprobarPerfume(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", APPROVED_MSG,
                    "data", convertToDto(perfume)
            ));
        } catch (Exception e) {
            log.error("Error aprobando perfume ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar un perfume")
    public ResponseEntity<?> rechazarPerfume(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String motivo = request.get("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", REASON_REQUIRED_MSG
                ));
            }

            log.info("Rechazando perfume ID {} por admin: {} con motivo: {}", id, userDetails.getUsername(), motivo);
            Perfume perfume = perfumeService.rechazarPerfume(id, motivo, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", PERFUME_DENIED_MSG,
                    "data", convertToDto(perfume)
            ));
        } catch (Exception e) {
            log.error("Error rechazando perfume ID {}: {}", id, e.getMessage(), e);
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
    @Operation(summary = "Actualizar perfume existente")
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
                                    fieldError.getDefaultMessage() : VALIDATIONS_ERRORS_MSG
                    ));

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", VALIDATIONS_ERRORS_MSG,
                            "errors", errores
                    ));
        }

        try {
            Perfume perfumeActualizado = perfumeService.actualizarPerfume(id, dto, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", UPDATED_PERFUME_MSG,
                    "data", convertToDto(perfumeActualizado),
                    "moderation", Map.of(
                            "status", perfumeActualizado.getModerationStatus(),
                            "message", perfumeActualizado.getModerationStatus() == ModerationStatus.APPROVED ?
                                    AUTOMATIC_APPROVED_MSG :
                                    perfumeActualizado.getRejectionReason() != null ? perfumeActualizado.getRejectionReason() : PERFUME_UNDER_REVIEW_MSG
                    ),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (RuntimeException e) {
            log.error("Error actualizando perfume ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Eliminar perfume")
    public ResponseEntity<?> eliminarPerfume(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Eliminando perfume ID {} por usuario: {}", id, userDetails.getUsername());
            perfumeService.eliminarPerfume(id, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", DELETED_PERFUME_MSG,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (RuntimeException e) {
            log.error("Error eliminando perfume ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}
