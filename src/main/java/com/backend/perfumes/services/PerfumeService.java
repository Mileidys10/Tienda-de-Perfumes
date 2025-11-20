package com.backend.perfumes.services;

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

import java.util.List;

@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public PerfumeService(PerfumeRepository perfumeRepository,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository) {
        this.perfumeRepository = perfumeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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

        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) {
            perfume.setImageUrl(dto.getImageUrl());
        } else {
            perfume.setImageUrl("/uploads/default-perfume.jpg");
        }

        System.out.println("🖼️ DEBUG - Setting image URL: " + perfume.getImageUrl());

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada con id: " + dto.getBrandId()));
        perfume.setBrand(brand);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con id: " + dto.getCategoryId()));
        perfume.setCategory(category);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        perfume.setUser(user);

        return perfumeRepository.save(perfume);
    }

    public Page<Perfume> listarPerfume(Pageable pageable, String filtro) {
        return perfumeRepository.findByFiltro(filtro, pageable);
    }

    public Page<Perfume> listarPerfumePorUsuario(String username, Pageable pageable, String filtro) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        return perfumeRepository.findByUserAndFiltro(user, filtro, pageable);
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
}