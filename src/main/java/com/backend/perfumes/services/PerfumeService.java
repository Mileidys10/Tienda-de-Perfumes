package com.backend.perfumes.services;

import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Brand;
import com.backend.perfumes.model.Category;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.repositories.BrandRepository;
import com.backend.perfumes.repositories.CategoryRepository;
import com.backend.perfumes.repositories.PerfumeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public PerfumeService(PerfumeRepository perfumeRepository,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository) {
        this.perfumeRepository = perfumeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Perfume savePerfume(PerfumeDTO dto, String username) {

        Perfume perfume = new Perfume();
        perfume.setName(dto.getName());
        perfume.setDescription(dto.getDescription());
        perfume.setPrice(dto.getPrice());
        perfume.setStock(dto.getStock());
        perfume.setSize_ml(dto.getSizeMl());
        perfume.setGenre(dto.getGenre());
        perfume.setRelease_date(dto.getReleaseDate());

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada con id: " + dto.getBrandId()));
        perfume.setBrand(brand);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con id: " + dto.getCategoryId()));
        perfume.setCategory(category);

        return perfumeRepository.save(perfume);
    }
}
