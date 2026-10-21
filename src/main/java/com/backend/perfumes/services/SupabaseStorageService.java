package com.backend.perfumes.services;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SupabaseStorageService {

    private final String supabaseUrl = "https://veydnrzpkxombrjsqqhg.supabase.co";
    private final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZleWRucnpwa3hvbWJyanNxcWhnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQxOTg5NTEsImV4cCI6MjA3OTc3NDk1MX0.JB3tUJVj33YLCUGk5ZAP1H2LM6xl2AKAOwyg1aDXslg";
    private final String bucketName = "perfumes";
    private final OkHttpClient client;
    private final String defaultImageUrl;

    public SupabaseStorageService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.defaultImageUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/default-perfume.jpg";
        log.info("🔄 SupabaseStorageService inicializado para: {}", supabaseUrl);
        initializeBucket();
    }

    private void initializeBucket() {
        try {
            // Verificar conexión con Supabase
            Request testRequest = new Request.Builder()
                    .url(supabaseUrl + "/storage/v1/bucket")
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .get()
                    .build();

            try (Response response = client.newCall(testRequest).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ Conexión con Supabase exitosa");
                } else {
                    log.error("❌ Error conectando con Supabase: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("❌ Error inicializando Supabase: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        log.info("📤 Iniciando upload de imagen...");

        try {
            // Validaciones
            if (file == null || file.isEmpty()) {
                throw new IOException("El archivo está vacío");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IOException("Tipo de archivo no soportado: " + contentType);
            }

            // Generar nombre único
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String fileName = "perfume_" + UUID.randomUUID() + fileExtension;

            log.info("📄 Subiendo archivo: {} ({} bytes, {})",
                    fileName, file.getSize(), contentType);

            // Crear request
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(file.getBytes(), MediaType.parse(contentType)))
                    .build();

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("Content-Type", "application/octet-stream")
                    .post(requestBody)
                    .build();

            log.info("🌐 Enviando a: {}/storage/v1/object/{}/{}", supabaseUrl, bucketName, fileName);

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response body";

                log.info("📨 Respuesta de Supabase - Código: {}", response.code());

                if (response.isSuccessful()) {
                    String imageUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
                    log.info("✅ Imagen subida exitosamente: {}", imageUrl);
                    return imageUrl;
                } else {
                    log.error("❌ Error de Supabase - Código: {}, Body: {}", response.code(), responseBody);

                    // Errores específicos de Supabase
                    if (response.code() == 401) {
                        throw new IOException("API Key inválida o expirada");
                    } else if (response.code() == 403) {
                        throw new IOException("Permisos insuficientes. Verifica las políticas del bucket");
                    } else if (response.code() == 404) {
                        throw new IOException("Bucket no encontrado: " + bucketName);
                    } else {
                        throw new IOException("Error de Supabase: " + response.code() + " - " + responseBody);
                    }
                }
            }

        } catch (Exception e) {
            log.error("💥 Error crítico en upload: {}", e.getMessage(), e);
            throw new IOException("Error subiendo imagen: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    public boolean deleteImage(String imageUrl) {
        try {
            if (isDefaultImage(imageUrl)) {
                return true;
            }

            String fileName = extractFileNameFromUrl(imageUrl);

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .delete()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                if (success) {
                    log.info("✅ Imagen eliminada: {}", fileName);
                } else {
                    log.error("❌ Error eliminando imagen: {}", response.code());
                }
                return success;
            }
        } catch (Exception e) {
            log.error("❌ Error eliminando imagen: {}", e.getMessage());
            return false;
        }
    }

    private String extractFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    public String getDefaultImageUrl() {
        return defaultImageUrl;
    }

    public boolean isDefaultImage(String imageUrl) {
        return imageUrl != null && imageUrl.equals(defaultImageUrl);
    }
}