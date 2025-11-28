package com.backend.perfumes.controller;

import com.backend.perfumes.services.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
@Slf4j
public class FileUploadController {

    private final SupabaseStorageService supabaseStorageService;

    public FileUploadController(SupabaseStorageService supabaseStorageService) {
        this.supabaseStorageService = supabaseStorageService;
    }

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("📥 Recibiendo solicitud de upload - Archivo: {}, Tamaño: {}, Tipo: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            if (file.isEmpty()) {
                log.warn("❌ Archivo vacío recibido");
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "El archivo está vacío"
                ));
            }

            String contentType = file.getContentType();
            log.info("🔍 Content-Type detectado: {}", contentType);

            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("❌ Tipo de archivo no permitido: {}", contentType);
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Solo se permiten archivos de imagen"
                ));
            }

            // Validar tamaño (máximo 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                log.warn("❌ Archivo demasiado grande: {} bytes", file.getSize());
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "La imagen es demasiado grande (máximo 5MB)"
                ));
            }

            log.info("🚀 Iniciando subida a Supabase...");

            // Subir a Supabase
            String imageUrl = supabaseStorageService.uploadImage(file);

            log.info("✅ Subida completada exitosamente. URL: {}", imageUrl);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Imagen subida exitosamente a Supabase",
                    "fileUrl", imageUrl,
                    "fileName", file.getOriginalFilename()
            ));

        } catch (Exception e) {
            log.error("💥 ERROR CRÍTICO al subir la imagen: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error al subir la imagen: " + e.getMessage(),
                    "debug", "Verifica la configuración de Supabase"
            ));
        }
    }


    @DeleteMapping("/image")
    public ResponseEntity<?> deleteImage(@RequestParam String imageUrl) {
        try {
            boolean deleted = supabaseStorageService.deleteImage(imageUrl);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Imagen eliminada exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "No se pudo eliminar la imagen"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error al eliminar la imagen: " + e.getMessage()
            ));
        }
    }
}