package com.backend.perfumes.controller;

import com.backend.perfumes.dto.BrandDTO;
import com.backend.perfumes.dto.PerfumeDTO;
import com.backend.perfumes.model.Brand;
import com.backend.perfumes.model.Genre;
import com.backend.perfumes.model.ModerationStatus;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.services.BrandService;
import com.backend.perfumes.services.PerfumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Marcas", description = "Gestion de marcas de perfumes")
public class BrandController {

    private final BrandService brandService;
    private final PerfumeService perfumeService;

    private static final String BRAND_CREATED_MSG = "Marca creada exitosamente";
    private static final String BRAND_CREATED_W_IMAGE_MSG = "Marca con imagen creada exitosamente";
    private static final String BRAND_AUTOMATIC_APPROVED_MSG = "Aprobada automaticamente";
    private static final String BRAND_APPROVED_MSG = "Marca aprobada exitosamente";
    private static final String BRAND_UNDER_REVIEW = "En revision";
    private static final String BRAND_REJECTED_MSG = "Marca rechazada exitosamente";
    private static final String REASON_REQUIRED_MSG = "Se requiere un motivo para rechazar";

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

    @PostMapping("/mis-marcas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Crear marca para el vendedor autenticado")
    public ResponseEntity<?> crearMiMarca(
            @RequestBody BrandDTO brandDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Creando marca '{}' para usuario: {}", brandDTO.getName(), userDetails.getUsername());
            Brand brand = new Brand();
            brand.setName(brandDTO.getName());
            brand.setDescription(brandDTO.getDescription());
            brand.setCountryOrigin(brandDTO.getCountryOrigin());
            brand.setImageUrl(brandDTO.getImageUrl());

            Brand nueva = brandService.crearBrand(brand, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", BRAND_CREATED_MSG,
                    "data", nueva,
                    "moderation", Map.of(
                            "status", nueva.getModerationStatus(),
                            "message", nueva.getModerationStatus() == ModerationStatus.APPROVED ?
                                    BRAND_AUTOMATIC_APPROVED_MSG :
                                    nueva.getRejectionReason() != null ? nueva.getRejectionReason() : BRAND_UNDER_REVIEW
                    )
            ));
        } catch (Exception e) {
            log.error("Error creando marca: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/mis-marcas/con-imagen", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Crear marca con imagen para el vendedor")
    public ResponseEntity<?> crearMiMarcaConImagen(
            @RequestPart("brand") BrandDTO brandDTO,
            @RequestPart("imagen") MultipartFile imagen,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Creando marca con imagen '{}' para usuario: {}", brandDTO.getName(), userDetails.getUsername());
            Brand brand = new Brand();
            brand.setName(brandDTO.getName());
            brand.setDescription(brandDTO.getDescription());
            brand.setCountryOrigin(brandDTO.getCountryOrigin());

            Brand nueva = brandService.crearBrandConImagen(brand, userDetails.getUsername(), imagen);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", BRAND_CREATED_W_IMAGE_MSG,
                    "data", nueva,
                    "moderation", Map.of(
                            "status", nueva.getModerationStatus(),
                            "message", nueva.getModerationStatus() == ModerationStatus.APPROVED ?
                                    BRAND_AUTOMATIC_APPROVED_MSG :
                                    nueva.getRejectionReason() != null ? nueva.getRejectionReason() : BRAND_UNDER_REVIEW
                    )
            ));
        } catch (Exception e) {
            log.error("Error creando marca con imagen: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mis-marcas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Listar marcas del vendedor")
    public ResponseEntity<?> listarMisMarcas(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "filtro", required = false) String filtro,
            @RequestParam(value = "status", required = false) ModerationStatus status) {
        try {
            List<Brand> marcas;
            if (filtro != null && !filtro.isEmpty()) {
                marcas = brandService.listarBrandsPorUsuarioConFiltro(userDetails.getUsername(), filtro);
            } else {
                marcas = brandService.listarBrandsPorUsuario(userDetails.getUsername());
            }

            if (status != null) {
                marcas = marcas.stream()
                        .filter(marca -> marca.getModerationStatus() == status)
                        .collect(Collectors.toList());
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
                        dto.setImageUrl(marca.getImageUrl());
                        dto.setModerationStatus(marca.getModerationStatus());
                        dto.setRejectionReason(marca.getRejectionReason());
                        dto.setPerfumes(perfumesDTO);
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marcasDTO,
                    "total", marcasDTO.size()
            ));
        } catch (Exception e) {
            log.error("Error listando mis marcas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mis-marcas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Obtener una marca del vendedor por ID")
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
            log.error("Error obteniendo mi marca ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mis-marcas/{brandId}/perfumes")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    @Operation(summary = "Obtener perfumes de una marca del vendedor")
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
                            "paisOrigen", marca.getCountryOrigin(),
                            "moderationStatus", marca.getModerationStatus()
                    ),
                    "data", perfumesDTO,
                    "total", perfumesDTO.size()
            ));
        } catch (Exception e) {
            log.error("Error obteniendo perfumes de marca ID {}: {}", brandId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/public")
    @Operation(summary = "Listar marcas publicas")
    public ResponseEntity<?> listarBrandsPublicas() {
        try {
            List<Brand> marcas = brandService.listarBrandsPublicas();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error listando marcas publicas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Obtener marca publica por ID")
    public ResponseEntity<?> obtenerBrandPublica(@PathVariable Long id) {
        try {
            Brand marca = brandService.obtenerBrandPublica(id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marca
            ));
        } catch (Exception e) {
            log.error("Error obteniendo marca publica ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/public/{brandId}/perfumes")
    @Operation(summary = "Obtener perfumes publicos de una marca")
    public ResponseEntity<?> obtenerPerfumesPublicosDeMarca(@PathVariable Long brandId) {
        try {
            List<Perfume> perfumes = perfumeService.obtenerPerfumesPublicosPorMarca(brandId);
            List<PerfumeDTO> perfumesDTO = perfumes.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", perfumesDTO,
                    "total", perfumesDTO.size()
            ));
        } catch (Exception e) {
            log.error("Error obteniendo perfumes publicos de marca {}: {}", brandId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las marcas para administrador")
    public ResponseEntity<?> listarTodasLasMarcasAdmin() {
        try {
            List<Brand> marcas = brandService.listarBrandsParaAdmin();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error listando marcas admin: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/admin/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar marcas pendientes de moderacion")
    public ResponseEntity<?> listarMarcasPendientes() {
        try {
            List<Brand> marcas = brandService.obtenerBrandsPendientes();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marcas,
                    "total", marcas.size()
            ));
        } catch (Exception e) {
            log.error("Error listando marcas pendientes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprobar una marca")
    public ResponseEntity<?> aprobarMarca(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Aprobando marca ID {} por admin: {}", id, userDetails.getUsername());
            Brand marca = brandService.aprobarBrand(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", BRAND_APPROVED_MSG,
                    "data", marca
            ));
        } catch (Exception e) {
            log.error("Error aprobando marca ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/admin/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechazar una marca")
    public ResponseEntity<?> rechazarMarca(
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

            log.info("Rechazando marca ID {} por admin: {} con motivo: {}", id, userDetails.getUsername(), motivo);
            Brand marca = brandService.rechazarBrand(id, motivo, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", BRAND_REJECTED_MSG,
                    "data", marca
            ));
        } catch (Exception e) {
            log.error("Error rechazando marca ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear marca como admin")
    public ResponseEntity<Brand> crearBrand(@RequestBody Brand brand) {
        Brand nueva = brandService.crearBrand(brand, "admin");
        return ResponseEntity.ok(nueva);
    }

    @GetMapping
    @Operation(summary = "Listar todas las marcas")
    public ResponseEntity<List<Brand>> listarBrands() {
        return ResponseEntity.ok(brandService.listarBrands());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener marca por ID")
    public ResponseEntity<Brand> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.obtenerBrandPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar marca por ID")
    public ResponseEntity<Brand> actualizarBrand(@PathVariable Long id, @RequestBody Brand brand) {
        return ResponseEntity.ok(brandService.actualizarBrand(id, brand));
    }

    @GetMapping("/check")
    @Operation(summary = "Verificar si el nombre de una marca ya existe")
    public ResponseEntity<?> checkBrandExists(@RequestParam("name") String name) {
        try {
            boolean exists = brandService.existsByName(name);
            return ResponseEntity.ok(Map.of(
                    "exists", exists,
                    "message", exists ? "La marca ya existe" : "Nombre disponible"
            ));
        } catch (Exception e) {
            log.error("Error verificando existencia de marca: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
