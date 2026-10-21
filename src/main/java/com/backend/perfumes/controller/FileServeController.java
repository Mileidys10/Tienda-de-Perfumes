package com.backend.perfumes.controller;

import com.backend.perfumes.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class FileServeController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final FileStorageService fileStorageService;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            String cleanFilename = Paths.get(filename).getFileName().toString();
            Path filePath = Paths.get(uploadDir).resolve(cleanFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(cleanFilename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .body(resource);
            } else {
                return serveDefaultImage(cleanFilename);
            }
        } catch (Exception e) {
            log.warn("Error sirviendo archivo {}: {}", filename, e.getMessage());
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
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lower.endsWith(".png")) {
                contentType = "image/png";
            } else if (lower.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (lower.endsWith(".webp")) {
                contentType = "image/webp";
            } else {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
        }
        return contentType;
    }

    private ResponseEntity<Resource> serveDefaultImage(String requestedFilename) {
        try {
            String defaultFilename = "default-brand.jpg";
            Path defaultPath = Paths.get(uploadDir).resolve(defaultFilename).normalize();
            Resource resource = new UrlResource(defaultPath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar la imagen por defecto: {}", e.getMessage());
        }

        return ResponseEntity.notFound().build();
    }
}
