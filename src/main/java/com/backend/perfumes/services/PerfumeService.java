package com.backend.perfumes.services;

import com.backend.perfumes.dto.ModerationResult;
import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.*;
import com.backend.perfumes.repositories.BrandRepository;
import com.backend.perfumes.repositories.CategoryRepository;
import com.backend.perfumes.repositories.PerfumeRepository;
import com.backend.perfumes.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AutoModerationService autoModerationService;
    private final SupabaseStorageService supabaseStorageService;

    // Constantes de moderación y auditoría
    private static final String AUTO_MODERATOR = "AUTO_MODERATOR";

    // Constantes de mensajes de error de dominio
    private static final String PERFUME_NOT_FOUND_MSG = "Perfume no encontrado";
    private static final String BRAND_NOT_FOUND_PREFIX_MSG = "Marca no encontrada con id: ";
    private static final String CATEGORY_NOT_FOUND_PREFIX_MSG = "Categoría no encontrada con id: ";
    private static final String USER_NOT_FOUND_PREFIX_MSG = "Usuario no encontrado: ";
    private static final String BRAND_NOT_FOUND_OR_NOT_OWNER_MSG = "Marca no encontrada o no pertenece al usuario";
    private static final String PERFUME_NOT_FOUND_OR_NOT_APPROVED_MSG = "Perfume no encontrado o no está aprobado";
    private static final String PERMISSION_DENIED_UPDATE_MSG = "No tienes permisos para actualizar este perfume";
    private static final String PERMISSION_DENIED_DELETE_MSG = "No tienes permisos para eliminar este perfume";

    // Constantes para logs
    private static final String LOG_PERFUME_SAVED = "Perfume '{}' creado y registrado con ID: {} por usuario: {}";
    private static final String LOG_PERFUME_UPDATED = "Perfume '{}' (ID: {}) actualizado por usuario: {}";
    private static final String LOG_PERFUME_DELETED = "Perfume ID: {} eliminado por usuario: {}";
    private static final String LOG_PERFUME_APPROVED = "Perfume ID: {} aprobado por admin: {}";
    private static final String LOG_PERFUME_REJECTED = "Perfume ID: {} rechazado por admin: {}. Motivo: {}";

    // -------------------------------------------------------------------------
    // Helper Methods (DRY)
    // -------------------------------------------------------------------------
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_PREFIX_MSG + username));
    }

    private Brand getBrandById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException(BRAND_NOT_FOUND_PREFIX_MSG + brandId));
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(CATEGORY_NOT_FOUND_PREFIX_MSG + categoryId));
    }

    private Perfume getPerfumeById(Long id) {
        return perfumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PERFUME_NOT_FOUND_MSG));
    }

    private void moderatePerfume(Perfume perfume) {
        ModerationResult result = autoModerationService.moderatePerfume(
                perfume.getName(), perfume.getDescription(), perfume.getPrice(),
                perfume.getStock(), perfume.getImageUrl());

        perfume.setModerationStatus(result.getStatus());
        perfume.setRejectionReason(result.getReason());
        perfume.setModerationDate(LocalDateTime.now());
        perfume.setModeratedBy(AUTO_MODERATOR);
    }

    // -------------------------------------------------------------------------
    // Operaciones Transaccionales de Dominio
    // -------------------------------------------------------------------------
    @Transactional
    public Perfume savePerfume(PerfumeDTO dto, String username) {
        Perfume perfume = new Perfume();
        perfume.setName(dto.getName());
        perfume.setDescription(dto.getDescription());
        perfume.setPrice(dto.getPrice());
        perfume.setStock(dto.getStock());
        perfume.setSizeMl(dto.getSizeMl());
        perfume.setGenre(dto.getGenre());
        perfume.setReleaseDate(dto.getReleaseDate());

        // Usar Supabase para imágenes
        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            perfume.setImageUrl(dto.getImageUrl());
        } else {
            perfume.setImageUrl(supabaseStorageService.getDefaultImageUrl());
        }

        perfume.setBrand(getBrandById(dto.getBrandId()));
        perfume.setCategory(getCategoryById(dto.getCategoryId()));
        perfume.setUser(getUserByUsername(username));

        moderatePerfume(perfume);

        Perfume saved = perfumeRepository.save(perfume);
        log.info(LOG_PERFUME_SAVED, saved.getName(), saved.getId(), username);
        return saved;
    }

    @Transactional
    public Perfume actualizarPerfume(Long id, PerfumeDTO dto, String username) {
        Perfume existente = getPerfumeById(id);
        User user = getUserByUsername(username);

        if (!existente.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException(PERMISSION_DENIED_UPDATE_MSG);
        }

        String oldImageUrl = existente.getImageUrl();

        // Actualizar campos básicos
        existente.setName(dto.getName());
        existente.setDescription(dto.getDescription());
        existente.setPrice(dto.getPrice());
        existente.setStock(dto.getStock());
        existente.setSizeMl(dto.getSizeMl());
        existente.setGenre(dto.getGenre());
        existente.setReleaseDate(dto.getReleaseDate());

        // Actualizar marca y categoría
        if (dto.getBrandId() != null) {
            existente.setBrand(getBrandById(dto.getBrandId()));
        }

        if (dto.getCategoryId() != null) {
            existente.setCategory(getCategoryById(dto.getCategoryId()));
        }

        // Actualizar imagen si viene en el DTO
        if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
            existente.setImageUrl(dto.getImageUrl());

            if (!dto.getImageUrl().equals(oldImageUrl) && !supabaseStorageService.isDefaultImage(oldImageUrl)) {
                supabaseStorageService.deleteImage(oldImageUrl);
            }
        }

        moderatePerfume(existente);

        Perfume updated = perfumeRepository.save(existente);
        log.info(LOG_PERFUME_UPDATED, updated.getName(), updated.getId(), username);
        return updated;
    }

    @Transactional
    public void eliminarPerfume(Long id, String username) {
        Perfume perfume = getPerfumeById(id);
        User user = getUserByUsername(username);

        if (!perfume.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException(PERMISSION_DENIED_DELETE_MSG);
        }

        String imageUrl = perfume.getImageUrl();
        if (!supabaseStorageService.isDefaultImage(imageUrl)) {
            supabaseStorageService.deleteImage(imageUrl);
        }

        perfumeRepository.delete(perfume);
        log.info(LOG_PERFUME_DELETED, id, username);
    }

    @Transactional
    public Perfume updatePerfumeImage(Long perfumeId, String imageUrl, String username) {
        Perfume perfume = getPerfumeById(perfumeId);
        User user = getUserByUsername(username);

        if (!perfume.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException(PERMISSION_DENIED_UPDATE_MSG);
        }

        String oldImageUrl = perfume.getImageUrl();
        perfume.setImageUrl(imageUrl);

        if (!supabaseStorageService.isDefaultImage(oldImageUrl)) {
            supabaseStorageService.deleteImage(oldImageUrl);
        }

        return perfumeRepository.save(perfume);
    }

    // -------------------------------------------------------------------------
    // Consultas y Moderación
    // -------------------------------------------------------------------------
    public Page<Perfume> listarPerfume(Pageable pageable, String filtro) {
        return perfumeRepository.findByModerationStatusAndFiltro(ModerationStatus.APPROVED, filtro, pageable);
    }

    public Page<Perfume> listarPerfumePorUsuario(String username, Pageable pageable, String filtro) {
        User user = getUserByUsername(username);
        return perfumeRepository.findByUserAndFiltro(user, filtro, pageable);
    }

    public Page<Perfume> listarPerfumesParaAdmin(Pageable pageable, String filtro) {
        return perfumeRepository.findByFiltro(filtro, pageable);
    }

    public List<Perfume> obtenerPerfumesPorMarcaYUsuario(Long brandId, String username, String filtro) {
        User user = getUserByUsername(username);

        Brand brand = brandRepository.findByIdAndUser(brandId, user)
                .orElseThrow(() -> new RuntimeException(BRAND_NOT_FOUND_OR_NOT_OWNER_MSG));

        if (filtro != null && !filtro.trim().isEmpty()) {
            return perfumeRepository.findByBrandAndUserWithFiltro(brandId, user, filtro);
        } else {
            return perfumeRepository.findByBrandIdAndUser(brandId, user);
        }
    }

    public List<Perfume> obtenerPerfumesPublicosPorMarca(Long brandId) {
        return perfumeRepository.findByBrandIdAndModerationStatus(brandId, ModerationStatus.APPROVED);
    }

    public Perfume obtenerPerfumePublico(Long id) {
        return perfumeRepository.findByIdAndModerationStatus(id, ModerationStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException(PERFUME_NOT_FOUND_OR_NOT_APPROVED_MSG));
    }

    public Perfume obtenerPerfumePorId(Long id) {
        return getPerfumeById(id);
    }

    public Perfume aprobarPerfume(Long id, String adminUsername) {
        Perfume perfume = getPerfumeById(id);
        perfume.setModerationStatus(ModerationStatus.APPROVED);
        perfume.setRejectionReason(null);
        perfume.setModerationDate(LocalDateTime.now());
        perfume.setModeratedBy(adminUsername);

        Perfume saved = perfumeRepository.save(perfume);
        log.info(LOG_PERFUME_APPROVED, id, adminUsername);
        return saved;
    }

    public Perfume rechazarPerfume(Long id, String motivo, String adminUsername) {
        Perfume perfume = getPerfumeById(id);
        perfume.setModerationStatus(ModerationStatus.REJECTED);
        perfume.setRejectionReason(motivo);
        perfume.setModerationDate(LocalDateTime.now());
        perfume.setModeratedBy(adminUsername);

        Perfume saved = perfumeRepository.save(perfume);
        log.info(LOG_PERFUME_REJECTED, id, adminUsername, motivo);
        return saved;
    }

    public List<Perfume> obtenerPerfumesPendientes() {
        return perfumeRepository.findByModerationStatus(ModerationStatus.PENDING_REVIEW);
    }

    public Page<Perfume> listarPerfumesPorEstado(ModerationStatus status, Pageable pageable) {
        return perfumeRepository.findByModerationStatus(status, pageable);
    }

    public Map<String, Long> obtenerEstadisticasModeracion(String username) {
        User user = getUserByUsername(username);

        long total = perfumeRepository.countByUser(user);
        long aprobados = perfumeRepository.countByUserAndModerationStatus(user, ModerationStatus.APPROVED);
        long pendientes = perfumeRepository.countByUserAndModerationStatus(user, ModerationStatus.PENDING_REVIEW);
        long rechazados = perfumeRepository.countByUserAndModerationStatus(user, ModerationStatus.REJECTED);
        long borradores = perfumeRepository.countByUserAndModerationStatus(user, ModerationStatus.DRAFT);

        return Map.of(
                "total", total,
                "approved", aprobados,
                "pending", pendientes,
                "rejected", rechazados,
                "draft", borradores
        );
    }
}
