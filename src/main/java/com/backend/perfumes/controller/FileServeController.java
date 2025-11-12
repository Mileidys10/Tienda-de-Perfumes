package com.backend.perfumes.controller;

import com.backend.perfumes.services.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@CrossOrigin(origins = "*")
public class FileServeController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileStorageService fileStorageService;

    public FileServeController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Limpiar el nombre del archivo por seguridad
            String cleanFilename = Paths.get(filename).getFileName().toString();

            Path filePath = Paths.get(uploadDir).resolve(cleanFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determinar el tipo de contenido
                String contentType = determineContentType(cleanFilename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(resource);
            } else {
                // Si el archivo no existe, servir imagen por defecto
                return serveDefaultImage(cleanFilename);
            }
        } catch (Exception e) {
            return serveDefaultImage(filename);
        }
    }

    private String determineContentType(String filename) {
        String contentType;
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (contentType == null) {
            if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            } else {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
        }
        return contentType;
    }

    private ResponseEntity<Resource> serveDefaultImage(String requestedFilename) {
        try {
            String defaultFilename;
            if (requestedFilename.contains("brand")) {
                defaultFilename = "default-brand.jpg";
            } else {
                defaultFilename = "default-brand.jpg";
            }

            Path defaultPath = Paths.get(uploadDir).resolve(defaultFilename).normalize();
            Resource resource = new UrlResource(defaultPath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            }
        } catch (Exception e) {
            // Fallback absoluto
        }

        // Si todo falla, retornar 404
        return ResponseEntity.notFound().build();
    }
}