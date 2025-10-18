package com.backend.perfumes.services;

import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.repositories.PerfumeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;

    public PerfumeService(PerfumeRepository perfumeRepository) {
        this.perfumeRepository = perfumeRepository;
    }

    @Transactional
    public Perfume crearPerfume(PerfumeDTO dto, String username) {

        if (dto.getPrice() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }

        if (dto.getStock() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }

        /*if (dto.getSize_ml() < 1) {
            throw new IllegalArgumentException("El tamaño debe ser al menos 1 ml");
        }*/

        /*Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new EntityNotFoundException("Marca no encontrada con ID: " + dto.getBrandId()));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + dto.getCategoryId()));*/

        Perfume perfume = new Perfume();
        perfume.setName(dto.getName());
        perfume.setDescription(dto.getDescription());
        perfume.setPrice(dto.getPrice());
        perfume.setStock(dto.getStock());
        //perfume.setSize_ml(dto.getSize_ml());
        perfume.setGenre(dto.getGenre());
        //perfume.setRelease_date(dto.getRelease_date());
        //perfume.setBrand(brand);
        //perfume.setCategory(category);

        return perfumeRepository.save(perfume);
    }
}
