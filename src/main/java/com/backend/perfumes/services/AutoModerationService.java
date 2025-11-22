package com.backend.perfumes.services;

import com.backend.perfumes.dto.ModerationResult;
import com.backend.perfumes.model.ModerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AutoModerationService {

    private final Set<String> BANNED_WORDS = Set.of(
            "porno", "porn", "sexo", "xxx", "adulto", "onlyfans", "explicit",
            "droga", "weed", "marihuana", "cocaína", "heroína", "lsd", "mdma",
            "matar", "asesinar", "violencia", "armas", "pistola", "disparar",
            "racista", "nazi", "hitler", "kkk", "odio", "discriminar",
            "estúpido", "idiota", "imbécil", "tonto", "retrasado",
            "estafa", "scam", "fraude", "timar", "engaño"
    );

    private final Set<String> SUSPICIOUS_WORDS = Set.of(
            "gratis", "free", "oferta", "descuento", "urgente", "inmediato",
            "ganar dinero", "trabajo desde casa", "millonario", "rico",
            "cripto", "bitcoin", "inversión", "forex", "apuesta"
    );

    private final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    private final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{9,15}\\b");

    public ModerationResult moderateBrand(String name, String description, String imageUrl) {
        log.info("Moderando marca: {}", name);

            if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return new ModerationResult(ModerationStatus.REJECTED, "Nombre y descripción son obligatorios");
        }

        if (name.trim().length() < 2) {
            return new ModerationResult(ModerationStatus.REJECTED, "El nombre es demasiado corto");
        }
        if (description.trim().length() < 10) {
            return new ModerationResult(ModerationStatus.REJECTED, "La descripción debe tener al menos 10 caracteres");
        }

        String nameLower = name.toLowerCase();
        String descLower = description.toLowerCase();

        for (String bannedWord : BANNED_WORDS) {
            if (nameLower.contains(bannedWord) || descLower.contains(bannedWord)) {
                return new ModerationResult(ModerationStatus.REJECTED,
                        "Contiene lenguaje inapropiado: " + bannedWord);
            }
        }

        boolean hasSuspicious = false;
        for (String suspiciousWord : SUSPICIOUS_WORDS) {
            if (nameLower.contains(suspiciousWord) || descLower.contains(suspiciousWord)) {
                hasSuspicious = true;
                break;
            }
        }

        if (containsUrlsOrContacts(description)) {
            return new ModerationResult(ModerationStatus.REJECTED,
                    "No se permiten URLs o información de contacto en la descripción");
        }

        if (imageUrl != null && !isValidImageUrl(imageUrl)) {
            return new ModerationResult(ModerationStatus.REJECTED,
                    "URL de imagen no válida");
        }

        if (hasSuspicious) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    "Contenido necesita revisión manual");
        }

        return new ModerationResult(ModerationStatus.APPROVED, "Aprobado automáticamente");
    }

    public ModerationResult moderatePerfume(String name, String description, Double price,
                                            Integer stock, String imageUrl) {
        log.info("Moderando perfume: {}", name);

        if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return new ModerationResult(ModerationStatus.REJECTED, "Nombre y descripción son obligatorios");
        }

        if (name.trim().length() < 2) {
            return new ModerationResult(ModerationStatus.REJECTED, "El nombre es demasiado corto");
        }
        if (description.trim().length() < 10) {
            return new ModerationResult(ModerationStatus.REJECTED, "La descripción debe tener al menos 10 caracteres");
        }

        if (price == null || price <= 0 || price > 10000) {
            return new ModerationResult(ModerationStatus.REJECTED, "Precio no válido");
        }
        if (stock == null || stock < 0) {
            return new ModerationResult(ModerationStatus.REJECTED, "Stock no válido");
        }

        String nameLower = name.toLowerCase();
        String descLower = description.toLowerCase();

        for (String bannedWord : BANNED_WORDS) {
            if (nameLower.contains(bannedWord) || descLower.contains(bannedWord)) {
                return new ModerationResult(ModerationStatus.REJECTED,
                        "Contiene lenguaje inapropiado: " + bannedWord);
            }
        }

        boolean hasSuspicious = false;
        for (String suspiciousWord : SUSPICIOUS_WORDS) {
            if (nameLower.contains(suspiciousWord) || descLower.contains(suspiciousWord)) {
                hasSuspicious = true;
                break;
            }
        }

        if (containsUrlsOrContacts(description)) {
            return new ModerationResult(ModerationStatus.REJECTED,
                    "No se permiten URLs o información de contacto en la descripción");
        }

        if (imageUrl != null && !isValidImageUrl(imageUrl)) {
            return new ModerationResult(ModerationStatus.REJECTED,
                    "URL de imagen no válida");
        }

        if (price < 1.0 || price > 5000.0) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    "Precio fuera de rango normal - necesita revisión");
        }

        if (stock > 10000) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    "Stock muy alto - necesita revisión");
        }

        if (hasSuspicious) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    "Contenido necesita revisión manual");
        }

        return new ModerationResult(ModerationStatus.APPROVED, "Aprobado automáticamente");
    }

    private boolean containsUrlsOrContacts(String text) {
        if (text == null) return false;

        return URL_PATTERN.matcher(text).find() ||
                EMAIL_PATTERN.matcher(text).find() ||
                PHONE_PATTERN.matcher(text).find();
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)(\\?.*)?$");
    }

    public void addBannedWord(String word) {
        BANNED_WORDS.add(word.toLowerCase());
    }

    public void removeBannedWord(String word) {
        BANNED_WORDS.remove(word.toLowerCase());
    }
}