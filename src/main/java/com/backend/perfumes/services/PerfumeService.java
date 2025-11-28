package com.backend.perfumes.services;

import com.backend.perfumes.dto.ModerationResult;
import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.*;
import com.backend.perfumes.repositories.BrandRepository;
import com.backend.perfumes.repositories.CategoryRepository;
import com.backend.perfumes.repositories.PerfumeRepository;
import com.backend.perfumes.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AutoModerationService autoModerationService;
    private final SupabaseStorageService supabaseStorageService;

    public PerfumeService(PerfumeRepository perfumeRepository,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          AutoModerationService autoModerationService,
                          SupabaseStorageService supabaseStorageService) {
        this.perfumeRepository = perfumeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.autoModerationService = autoModerationService;
        this.supabaseStorageService = supabaseStorageService;
    }

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
            // Imagen por defecto de Supabase
            perfume.setImageUrl(supabaseStorageService.getDefaultImageUrl());
        }

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada con id: " + dto.getBrandId()));
        perfume.setBrand(brand);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con id: " + dto.getCategoryId()));
        perfume.setCategory(category);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        perfume.setUser(user);

        ModerationResult result = autoModerationService.moderatePerfume(
                perfume.getName(), perfume.getDescription(), perfume.getPrice(),
                perfume.getStock(), perfume.getImageUrl());

        perfume.setModerationStatus(result.getStatus());
        perfume.setRejectionReason(result.getReason());
        perfume.setModerationDate(LocalDateTime.now());
        perfume.setModeratedBy("AUTO_MODERATOR");

        return perfumeRepository.save(perfume);
    }

    @Transactional
    public Perfume actualizarPerfume(Long id, PerfumeDTO dto, String username) {
        Perfume existente = perfumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (!existente.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("No tienes permisos para actualizar este perfume");
        }

        // Guardar imagen anterior para posible eliminación
        String oldImageUrl = existente.getImageUrl();

        existente.setName(dto.getName());
        existente.setDescription(dto.getDescription());
        existente.setPrice(dto.getPrice());
        existente.setStock(dto.getStock());
        existente.setSizeMl(dto.getSizeMl());
        existente.setGenre(dto.getGenre());
        existente.setReleaseDate(dto.getReleaseDate());

        // Actualizar imagen si es diferente
        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty() &&
                !dto.getImageUrl().equals(oldImageUrl)) {
            existente.setImageUrl(dto.getImageUrl());

            // Eliminar imagen anterior si no es la por defecto
            if (!supabaseStorageService.isDefaultImage(oldImageUrl)) {
                supabaseStorageService.deleteImage(oldImageUrl);
            }
        }

        ModerationResult result = autoModerationService.moderatePerfume(
                existente.getName(), existente.getDescription(), existente.getPrice(),
                existente.getStock(), existente.getImageUrl());

        existente.setModerationStatus(result.getStatus());
        existente.setRejectionReason(result.getReason());
        existente.setModerationDate(LocalDateTime.now());
        existente.setModeratedBy("AUTO_MODERATOR");

        return perfumeRepository.save(existente);
    }

    @Transactional
    public void eliminarPerfume(Long id, String username) {
        Perfume perfume = perfumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (!perfume.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("No tienes permisos para eliminar este perfume");
        }

        // Eliminar imagen de Supabase si no es la por defecto
        String imageUrl = perfume.getImageUrl();
        if (!supabaseStorageService.isDefaultImage(imageUrl)) {
            supabaseStorageService.deleteImage(imageUrl);
        }

        perfumeRepository.delete(perfume);
    }

    // Método específico para actualizar solo la imagen
    @Transactional
    public Perfume updatePerfumeImage(Long perfumeId, String imageUrl, String username) {
        Perfume perfume = perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (!perfume.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("No tienes permisos para editar este perfume");
        }

        // Guardar imagen anterior
        String oldImageUrl = perfume.getImageUrl();

        // Actualizar imagen
        perfume.setImageUrl(imageUrl);

        // Eliminar imagen anterior si no es la por defecto
        if (!supabaseStorageService.isDefaultImage(oldImageUrl)) {
            supabaseStorageService.deleteImage(oldImageUrl);
        }

        return perfumeRepository.save(perfume);
    }

    // Resto de métodos se mantienen igual...
    public Page<Perfume> listarPerfume(Pageable pageable, String filtro) {
        return perfumeRepository.findByModerationStatusAndFiltro(ModerationStatus.APPROVED, filtro, pageable);
    }

    public Page<Perfume> listarPerfumePorUsuario(String username, Pageable pageable, String filtro) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        return perfumeRepository.findByUserAndFiltro(user, filtro, pageable);
    }

    public Page<Perfume> listarPerfumesParaAdmin(Pageable pageable, String filtro) {
        return perfumeRepository.findByFiltro(filtro, pageable);
    }

    public List<Perfume> obtenerPerfumesPorMarcaYUsuario(Long brandId, String username, String filtro) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        Brand brand = brandRepository.findByIdAndUser(brandId, user)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada o no pertenece al usuario"));

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
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado o no está aprobado"));
    }

    public Perfume obtenerPerfumePorId(Long id) {
        return perfumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));
    }

    public Perfume aprobarPerfume(Long id, String adminUsername) {
        Perfume perfume = perfumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));
        perfume.setModerationStatus(ModerationStatus.APPROVED);
        perfume.setRejectionReason(null);
        perfume.setModerationDate(LocalDateTime.now());
        perfume.setModeratedBy(adminUsername);
        return perfumeRepository.save(perfume);
    }

    public Perfume rechazarPerfume(Long id, String motivo, String adminUsername) {
        Perfume perfume = perfumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));
        perfume.setModerationStatus(ModerationStatus.REJECTED);
        perfume.setRejectionReason(motivo);
        perfume.setModerationDate(LocalDateTime.now());
        perfume.setModeratedBy(adminUsername);
        return perfumeRepository.save(perfume);
    }

    public List<Perfume> obtenerPerfumesPendientes() {
        return perfumeRepository.findByModerationStatus(ModerationStatus.PENDING_REVIEW);
    }

    public Page<Perfume> listarPerfumesPorEstado(ModerationStatus status, Pageable pageable) {
        return perfumeRepository.findByModerationStatus(status, pageable);
    }

    public Map<String, Long> obtenerEstadisticasModeracion(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

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