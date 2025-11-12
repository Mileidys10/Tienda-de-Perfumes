package com.backend.perfumes.controller;

import com.backend.perfumes.services.FileStorageService;
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

            // Subir archivo y obtener solo el nombre del archivo
            String fileName = fileStorageService.storeFile(file);

            System.out.println("File uploaded successfully: " + fileName);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Imagen subida exitosamente",
                    "fileUrl", fileName // Solo el nombre del archivo
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