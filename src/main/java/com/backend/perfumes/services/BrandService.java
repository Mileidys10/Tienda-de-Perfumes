package com.backend.perfumes.services;

import com.backend.perfumes.dto.ModerationResult;
import com.backend.perfumes.model.Brand;
import com.backend.perfumes.model.ModerationStatus;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.BrandRepository;
import com.backend.perfumes.repositories.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AutoModerationService autoModerationService;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository,
                        FileStorageService fileStorageService, AutoModerationService autoModerationService) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.autoModerationService = autoModerationService;
    }

    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado: ";

    private static final String AUTO_MODERATOR = "AUTO_MODERATOR";



    private static final String BRAND_NOT_FOUND_OR_NOT_OWNER_MSG =
            "Marca no encontrada o no pertenece al usuario";

    private static final String BRAND_NOT_FOUND_OR_NOT_APPROVED_MSG =
            "Marca no encontrada o no está aprobada";

    private static final String BRAND_NOT_FOUND_MSG =
            "Marca no encontrada";




    private User getUserByUsername(String username){

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG + username));

    }

private void moderateBrand(Brand brand){
    ModerationResult result = autoModerationService.moderateBrand(
            brand.getName(), brand.getDescription(), brand.getImageUrl());

    brand.setModerationStatus(result.getStatus());
    brand.setRejectionReason(result.getReason());
    brand.setModerationDate(LocalDateTime.now());
    brand.setModeratedBy(AUTO_MODERATOR);

}


    public Brand crearBrand(Brand brand, String username) {


        User user = getUserByUsername(username);
        brand.setUser(user);

        moderateBrand(brand);

        if (brand.getImageUrl() == null || brand.getImageUrl().isEmpty()) {
            brand.setImageUrl(fileStorageService.getDefaultBrandImageUrl());
        }

        return brandRepository.save(brand);
    }

    public Brand crearBrandConImagen(Brand brand, String username, MultipartFile imagen) throws IOException {
        User user = getUserByUsername(username);
        brand.setUser(user);



      moderateBrand(brand);
        if (imagen != null && !imagen.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(imagen);
            brand.setImageUrl(imageUrl);
        } else {
            brand.setImageUrl(fileStorageService.getDefaultBrandImageUrl());
        }
        return brandRepository.save(brand);
    }

    public List<Brand> listarBrandsPorUsuario(String username) {
        User user = getUserByUsername(username);
        return brandRepository.findByUser(user);
    }

    public List<Brand> listarBrandsPorUsuarioConFiltro(String username, String filtro) {
        User user = getUserByUsername(username);
        return brandRepository.findByUserAndFiltro(user, filtro);
    }

    public List<Brand> listarBrandsPublicas() {
        return brandRepository.findByModerationStatus(ModerationStatus.APPROVED);
    }

    public List<Brand> listarBrandsParaAdmin() {
        return brandRepository.findAll();
    }

    public Brand obtenerBrandPorIdYUsuario(Long id, String username) {
        User user = getUserByUsername(username);
        return brandRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException(BRAND_NOT_FOUND_OR_NOT_OWNER_MSG));
    }

    public Brand obtenerBrandPublica(Long id) {
        return brandRepository.findByIdAndModerationStatus(id, ModerationStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException(BRAND_NOT_FOUND_OR_NOT_APPROVED_MSG));
    }

    public void eliminarBrand(Long id, String username) {
        Brand brand = obtenerBrandPorIdYUsuario(id, username);
        brandRepository.delete(brand);
    }

    public List<Brand> listarBrands() {
        return brandRepository.findByModerationStatus(ModerationStatus.APPROVED);
    }

    public Brand obtenerBrandPorId(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(BRAND_NOT_FOUND_MSG));
    }

    public Brand actualizarBrand(Long id, Brand brand) {
        Brand existente = obtenerBrandPorId(id);
        existente.setName(brand.getName());
        existente.setDescription(brand.getDescription());
        existente.setCountryOrigin(brand.getCountryOrigin());

        if (brand.getImageUrl() != null && !brand.getImageUrl().isEmpty()) {
            existente.setImageUrl(brand.getImageUrl());
        }

    moderateBrand(existente);

        return brandRepository.save(existente);
    }

    public Brand aprobarBrand(Long id, String adminUsername) {
        Brand brand = obtenerBrandPorId(id);
        brand.setModerationStatus(ModerationStatus.APPROVED);
        brand.setRejectionReason(null);
        brand.setModerationDate(LocalDateTime.now());
        brand.setModeratedBy(adminUsername);
        return brandRepository.save(brand);
    }

    public Brand rechazarBrand(Long id, String motivo, String adminUsername) {
        Brand brand = obtenerBrandPorId(id);
        brand.setModerationStatus(ModerationStatus.REJECTED);
        brand.setRejectionReason(motivo);
        brand.setModerationDate(LocalDateTime.now());
        brand.setModeratedBy(adminUsername);
        return brandRepository.save(brand);
    }

    public List<Brand> obtenerBrandsPendientes() {
        return brandRepository.findByModerationStatus(ModerationStatus.PENDING_REVIEW);
    }

    public boolean existsByName(String name) {
        return brandRepository.existsByName(name);
    }
}