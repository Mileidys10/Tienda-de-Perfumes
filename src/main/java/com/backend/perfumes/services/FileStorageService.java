package com.backend.perfumes.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    private static final String DEFAULT_BRAND_IMAGE_URL ="/uploads/default-brand.jpg";
    private static final String DEFAULT_BRAND_PERFUME_IMAGE_URL ="/uploads/default-perfume.jpg";
    private static final String EMPTY_FILE_MSG="el archivo esta vacio";
    private static final String MAIN_FILE_URL="/uploads/";
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException(EMPTY_FILE_MSG);
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath);

        return MAIN_FILE_URL + fileName;
    }

    public void deleteFile(String fileUrl) throws IOException {
        if (fileUrl != null && fileUrl.startsWith(MAIN_FILE_URL)) {
            String fileName = fileUrl.substring(MAIN_FILE_URL.length());
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        }
    }

    public String getDefaultBrandImageUrl() {
        return DEFAULT_BRAND_IMAGE_URL;
    }

    public String getDefaultPerfumeImageUrl() {
        return DEFAULT_BRAND_PERFUME_IMAGE_URL;
    }
}