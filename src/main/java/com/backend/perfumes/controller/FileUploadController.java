package com.backend.perfumes.controller;

import com.backend.perfumes.services.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final SupabaseStorageService supabaseStorageService;

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L; // 5MB
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

    private static final String EMPTY_FILE_MSG = "El archivo esta vacio";
    private static final String INVALID_FILE_TYPE_MSG = "Solo se permiten archivos de imagen";
    private static final String FILE_TOO_LARGE_MSG = "La imagen es demasiado grande (maximo 5MB)";
    private static final String UPLOAD_SUCCESS_MSG = "Imagen subida exitosamente a Supabase";
    private static final String DELETE_SUCCESS_MSG = "Imagen eliminada exitosamente";
    private static final String DELETE_FAIL_MSG = "No se pudo eliminar la imagen";

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("Recibiendo solicitud de subida - Archivo: {}, Tamano: {} bytes, Tipo: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            if (file.isEmpty()) {
                log.warn("Archivo vacio recibido");
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", EMPTY_FILE_MSG
                ));
            }

            String contentType = file.getContentType();
            log.info("Content-Type detectado: {}", contentType);

            if (contentType == null || !contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
                log.warn("Tipo de archivo no permitido: {}", contentType);
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", INVALID_FILE_TYPE_MSG
                ));
            }

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                log.warn("Archivo demasiado grande: {} bytes", file.getSize());
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", FILE_TOO_LARGE_MSG
                ));
            }

            log.info("Iniciando subida de imagen a Supabase...");
            String imageUrl = supabaseStorageService.uploadImage(file);
            log.info("Subida completada exitosamente. URL: {}", imageUrl);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", UPLOAD_SUCCESS_MSG,
                    "fileUrl", imageUrl,
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : ""
            ));

        } catch (Exception e) {
            log.error("Error critico al subir imagen: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error al subir la imagen: " + e.getMessage(),
                    "debug", "Verifica la configuracion de Supabase"
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
                        "message", DELETE_SUCCESS_MSG
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", DELETE_FAIL_MSG
                ));
            }

        } catch (Exception e) {
            log.error("Error al eliminar imagen: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error al eliminar la imagen: " + e.getMessage()
            ));
        }
    }
}
