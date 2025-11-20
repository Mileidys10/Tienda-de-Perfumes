package com.backend.perfumes.controller;

import com.backend.perfumes.services.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        System.out.println("=== UPLOAD DEBUG ===");
        System.out.println("File name: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize());
        System.out.println("Content type: " + file.getContentType());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "El archivo está vacío"
                ));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Solo se permiten archivos de imagen"
                ));
            }

            String filePath = fileStorageService.storeFile(file);

            String fullImageUrl = serverUrl + filePath;

            System.out.println("✅ File uploaded successfully: " + filePath);
            System.out.println("🔗 Full image URL: " + fullImageUrl);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Imagen subida exitosamente",
                    "filePath", filePath,
                    "fileUrl", fullImageUrl, // URL COMPLETA para el frontend
                    "fileName", file.getOriginalFilename()
            ));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Error al subir la imagen: " + e.getMessage()
            ));
        }
    }
}