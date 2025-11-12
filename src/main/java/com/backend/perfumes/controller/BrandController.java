package com.backend.perfumes.controller;

import com.backend.perfumes.dto.BrandDTO;
import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Brand;
import com.backend.perfumes.model.Genre;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.services.BrandService;
import com.backend.perfumes.services.PerfumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "*")
public class BrandController {

    private final BrandService brandService;
    private final PerfumeService perfumeService;

    public BrandController(BrandService brandService, PerfumeService perfumeService) {
        this.brandService = brandService;
        this.perfumeService = perfumeService;
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

    @PostMapping("/mis-marcas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> crearMiMarca(
            @RequestBody BrandDTO brandDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Brand brand = new Brand();
            brand.setName(brandDTO.getName());
            brand.setDescription(brandDTO.getDescription());
            brand.setCountryOrigin(brandDTO.getCountryOrigin());
            brand.setImageUrl(brandDTO.getImageUrl());

            Brand nueva = brandService.crearBrand(brand, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Marca creada exitosamente",
                    "data", nueva
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/mis-marcas/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> crearMiMarcaConImagen(
            @RequestPart("brand") BrandDTO brandDTO,
            @RequestPart("imagen") MultipartFile imagen,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Brand brand = new Brand();
            brand.setName(brandDTO.getName());
            brand.setDescription(brandDTO.getDescription());
            brand.setCountryOrigin(brandDTO.getCountryOrigin());

            Brand nueva = brandService.crearBrandConImagen(brand, userDetails.getUsername(), imagen);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Marca con imagen creada exitosamente",
                    "data", nueva
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mis-marcas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> listarMisMarcas(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "filtro", required = false) String filtro) {
        try {
            List<Brand> marcas;
            if (filtro != null && !filtro.isEmpty()) {
                marcas = brandService.listarBrandsPorUsuarioConFiltro(userDetails.getUsername(), filtro);
            } else {
                marcas = brandService.listarBrandsPorUsuario(userDetails.getUsername());
            }

            List<BrandDTO> marcasDTO = marcas.stream()
                    .map(marca -> {
                        List<Perfume> perfumes = perfumeService.obtenerPerfumesPorMarcaYUsuario(
                                marca.getId(), userDetails.getUsername(), null);

                        List<PerfumeDTO> perfumesDTO = perfumes.stream()
                                .map(this::convertToDto)
                                .collect(Collectors.toList());

                        BrandDTO dto = new BrandDTO();
                        dto.setId(marca.getId());
                        dto.setName(marca.getName());
                        dto.setDescription(marca.getDescription());
                        dto.setCountryOrigin(marca.getCountryOrigin());
                        dto.setCreador(marca.getUser().getUsername());
                        dto.setPerfumes(perfumesDTO);
                        dto.setTotalPerfumes(perfumesDTO.size());
                        dto.setImageUrl(marca.getImageUrl());

                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marcasDTO,
                    "total", marcasDTO.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mis-marcas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> obtenerMiMarca(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Brand marca = brandService.obtenerBrandPorIdYUsuario(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marca
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mis-marcas/{brandId}/perfumes")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> obtenerPerfumesDeMiMarca(
            @PathVariable Long brandId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "filtro", required = false) String filtro) {
        try {
            List<Perfume> perfumes = perfumeService.obtenerPerfumesPorMarcaYUsuario(
                    brandId, userDetails.getUsername(), filtro);

            List<PerfumeDTO> perfumesDTO = perfumes.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            Brand marca = brandService.obtenerBrandPorIdYUsuario(brandId, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "marca", Map.of(
                            "id", marca.getId(),
                            "nombre", marca.getName(),
                            "descripcion", marca.getDescription(),
                            "paisOrigen", marca.getCountryOrigin()
                    ),
                    "data", perfumesDTO,
                    "total", perfumesDTO.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Brand> crearBrand(@RequestBody Brand brand) {
        Brand nueva = brandService.crearBrand(brand, "admin");
        return ResponseEntity.ok(nueva);
    }

    @GetMapping
    public ResponseEntity<List<Brand>> listarBrands() {
        return ResponseEntity.ok(brandService.listarBrands());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Brand> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.obtenerBrandPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Brand> actualizarBrand(@PathVariable Long id, @RequestBody Brand brand) {
        return ResponseEntity.ok(brandService.actualizarBrand(id, brand));
    }

    /*@DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> eliminarBrand(@PathVariable Long id) {
        brandService.eliminarBrand(id);
        return ResponseEntity.ok("Marca eliminada correctamente.");
    }*/

    @GetMapping("/check")
    public ResponseEntity<?> checkBrandExists(@RequestParam("name") String name) {
        try {
            boolean exists = brandService.existsByName(name);
            return ResponseEntity.ok(Map.of(
                    "exists", exists,
                    "message", exists ? "La marca ya existe" : "Nombre disponible"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}