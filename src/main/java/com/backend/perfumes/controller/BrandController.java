package com.backend.perfumes.controller;

import com.backend.perfumes.model.Brand;
import com.backend.perfumes.services.BrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "*")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    public ResponseEntity<Brand> crearBrand(@RequestBody Brand brand) {
        Brand nueva = brandService.crearBrand(brand);
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
    public ResponseEntity<Brand> actualizarBrand(@PathVariable Long id, @RequestBody Brand brand) {
        return ResponseEntity.ok(brandService.actualizarBrand(id, brand));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarBrand(@PathVariable Long id) {
        brandService.eliminarBrand(id);
        return ResponseEntity.ok("Marca eliminada correctamente.");
    }
}
