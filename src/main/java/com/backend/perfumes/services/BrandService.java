package com.backend.perfumes.services;

import com.backend.perfumes.model.Brand;
import com.backend.perfumes.repositories.BrandRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public Brand crearBrand(Brand brand) {
        try {
            return brandRepository.save(brand);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ya existe una marca con ese nombre.");
        }
    }

    public List<Brand> listarBrands() {
        return brandRepository.findAll();
    }

    public Brand obtenerBrandPorId(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Marca no encontrada con ID: " + id));
    }

    public void eliminarBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new EntityNotFoundException("No existe la marca con ID: " + id);
        }
        brandRepository.deleteById(id);
    }

    public Brand actualizarBrand(Long id, Brand brandActualizada) {
        Brand brandExistente = obtenerBrandPorId(id);
        brandExistente.setName(brandActualizada.getName());
        return brandRepository.save(brandExistente);
    }
}
