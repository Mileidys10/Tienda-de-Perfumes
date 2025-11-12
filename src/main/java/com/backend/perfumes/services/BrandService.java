package com.backend.perfumes.services;

import com.backend.perfumes.model.Brand;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.BrandRepository;
import com.backend.perfumes.repositories.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository, FileStorageService fileStorageService) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public Brand crearBrand(Brand brand, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        brand.setUser(user);
        if (brand.getImageUrl() == null || brand.getImageUrl().isEmpty()) {
            brand.setImageUrl(fileStorageService.getDefaultBrandImageUrl());
        }
        return brandRepository.save(brand);
    }

    public Brand crearBrandConImagen(Brand brand, String username, MultipartFile imagen) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        brand.setUser(user);

        if (imagen != null && !imagen.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(imagen);
            brand.setImageUrl(imageUrl);
        } else {
            brand.setImageUrl("/uploads/default-brand.jpg");
        }

        return brandRepository.save(brand);
    }

    /*
    public Brand actualizarBrandConImagen(Long id, Brand brand, String username, MultipartFile imagen) throws IOException {
        Brand existente = obtenerBrandPorIdYUsuario(id, username);

        existente.setName(brand.getName());
        existente.setDescription(brand.getDescription());
        existente.setCountryOrigin(brand.getCountryOrigin());

        if (imagen != null && !imagen.isEmpty()) {
            if (existente.getImageUrl() != null &&
                    !existente.getImageUrl().equals("/uploads/default-brand.jpg")) {
                fileStorageService.deleteFile(existente.getImageUrl());
            }

            String imageUrl = fileStorageService.storeFile(imagen);
            existente.setImageUrl(imageUrl);
        }

        return brandRepository.save(existente);
    }
*/


    public List<Brand> listarBrandsPorUsuario(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return brandRepository.findByUser(user);
    }

    public List<Brand> listarBrandsPorUsuarioConFiltro(String username, String filtro) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return brandRepository.findByUserAndFiltro(user, filtro);
    }

    public Brand obtenerBrandPorIdYUsuario(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return brandRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada o no pertenece al usuario"));
    }


    public void eliminarBrand(Long id, String username) {
        Brand brand = obtenerBrandPorIdYUsuario(id, username);

        /*if (brand.getImageUrl() != null &&
                !brand.getImageUrl().equals("/uploads/default-brand.jpg")) {
            fileStorageService.deleteFile(brand.getImageUrl());
        }*/

        brandRepository.delete(brand);
    }

    public List<Brand> listarBrands() {
        return brandRepository.findAll();
    }

    public Brand obtenerBrandPorId(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
    }

    public Brand actualizarBrand(Long id, Brand brand) {
        Brand existente = obtenerBrandPorId(id);
        existente.setName(brand.getName());
        existente.setDescription(brand.getDescription());
        existente.setCountryOrigin(brand.getCountryOrigin());

        if (brand.getImageUrl() != null && !brand.getImageUrl().isEmpty()) {
            existente.setImageUrl(brand.getImageUrl());
        }

        return brandRepository.save(existente);
    }

    /*

    public void eliminarBrand(Long id) {
        Brand brand = obtenerBrandPorId(id);

        if (brand.getImageUrl() != null &&
                !brand.getImageUrl().equals("/uploads/default-brand.jpg")) {
            fileStorageService.deleteFile(brand.getImageUrl());
        }

        brandRepository.deleteById(id);
    }*/

    public boolean existsByName(String name) {
        return brandRepository.existsByName(name);
    }
}