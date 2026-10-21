package com.backend.perfumes.services;

import com.backend.perfumes.dto.ModerationResult;
import com.backend.perfumes.model.ModerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AutoModerationService {

    private final Set<String> BANNED_WORDS =  new HashSet<>(Set.of(
            "porno", "porn", "sexo", "xxx", "adulto", "onlyfans", "explicit",
            "droga", "weed", "marihuana", "cocaína", "heroína", "lsd", "mdma",
            "matar", "asesinar", "violencia", "armas", "pistola", "disparar",
            "racista", "nazi", "hitler", "kkk", "odio", "discriminar",
            "estúpido", "idiota", "imbécil", "tonto", "retrasado",
            "estafa", "scam", "fraude", "timar", "engaño"
    ));

    private final  Set<String> SUSPICIOUS_WORDS = new HashSet<>(Set.of(
            "gratis", "free", "oferta", "descuento", "urgente", "inmediato",
            "ganar dinero", "trabajo desde casa", "millonario", "rico",
            "cripto", "bitcoin", "inversión", "forex", "apuesta"
    ));

    private final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    private final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{9,15}\\b");

    private static final String REQUIRED_NAME_DESCRIPTION_MSG =
            "Nombre y descripción son obligatorios";

    private static final String NAME_TOO_SHORT_MSG =
            "El nombre es demasiado corto";

    private static final String DESCRIPTION_TOO_SHORT_MSG =
            "La descripción debe tener al menos 10 caracteres";

    private static final String INAPPROPRIATE_LANGUAGE_MSG =
            "Contiene lenguaje inapropiado: ";

    private static final String URLS_OR_CONTACTS_NOT_ALLOWED_MSG =
            "No se permiten URLs o información de contacto en la descripción";

    private static final String INVALID_IMAGE_URL_MSG =
            "URL de imagen no válida";

    private static final String MANUAL_REVIEW_REQUIRED_MSG =
            "Contenido necesita revisión manual";

    private static final String AUTO_APPROVED_MSG =
            "Aprobado automáticamente";

    private static final String INVALID_PRICE_MSG =
            "Precio no válido";

    private static final String INVALID_STOCK_MSG =
            "Stock no válido";

    private static final String PRICE_OUT_OF_RANGE_MSG =
            "Precio fuera de rango normal - necesita revisión";

    private static final String HIGH_STOCK_REVIEW_MSG =
            "Stock muy alto - necesita revisión";

    private static final String MODERATING_BRAND_MSG =
            "Moderando marca: {}";

    private static final String MODERATING_PERFUME_MSG =
            "Moderando perfume: {}";

    private static final int MIN_NAME_LENGTH = 2;

    private static final int MIN_DESCRIPTION_LENGTH = 10;

    private static final double MAX_ALLOWED_PRICE = 10000.0;

    private static final double MIN_NORMAL_PRICE = 1.0;

    private static final double MAX_NORMAL_PRICE = 5000.0;

    private static final int MAX_NORMAL_STOCK = 10000;


    public ModerationResult moderateBrand(String name, String description, String imageUrl) {
        log.info(MODERATING_BRAND_MSG, name);

            if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return new ModerationResult(ModerationStatus.REJECTED, REQUIRED_NAME_DESCRIPTION_MSG);
        }

        if (name.trim().length() < MIN_NAME_LENGTH) {
            return new ModerationResult(ModerationStatus.REJECTED, NAME_TOO_SHORT_MSG);
        }
        if (description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            return new ModerationResult(ModerationStatus.REJECTED, DESCRIPTION_TOO_SHORT_MSG );
        }

        String nameLower = name.toLowerCase();
        String descLower = description.toLowerCase();

        for (String bannedWord : BANNED_WORDS) {
            if (nameLower.contains(bannedWord) || descLower.contains(bannedWord)) {
                return new ModerationResult(ModerationStatus.REJECTED,
                        INAPPROPRIATE_LANGUAGE_MSG + bannedWord);
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
                    URLS_OR_CONTACTS_NOT_ALLOWED_MSG);
        }

        if (imageUrl != null && !isValidImageUrl(imageUrl)) {
            return new ModerationResult(ModerationStatus.REJECTED,
                    INVALID_IMAGE_URL_MSG);
        }

        if (hasSuspicious) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    MANUAL_REVIEW_REQUIRED_MSG);
        }

        return new ModerationResult(ModerationStatus.APPROVED, AUTO_APPROVED_MSG);
    }

    public ModerationResult moderatePerfume(String name, String description, Double price,
                                            Integer stock, String imageUrl) {
        log.info(MODERATING_PERFUME_MSG, name);

        if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return new ModerationResult(ModerationStatus.REJECTED, REQUIRED_NAME_DESCRIPTION_MSG);
        }

        if (name.trim().length() < MIN_NAME_LENGTH) {
            return new ModerationResult(ModerationStatus.REJECTED, NAME_TOO_SHORT_MSG);
        }
        if (description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            return new ModerationResult(ModerationStatus.REJECTED, DESCRIPTION_TOO_SHORT_MSG );
        }

        if (price == null || price <= 0 || price > MAX_NORMAL_STOCK) {
            return new ModerationResult(ModerationStatus.REJECTED, INVALID_PRICE_MSG);
        }
        if (stock == null || stock < 0) {
            return new ModerationResult(ModerationStatus.REJECTED, INVALID_STOCK_MSG );
        }

        String nameLower = name.toLowerCase();
        String descLower = description.toLowerCase();

        for (String bannedWord : BANNED_WORDS) {
            if (nameLower.contains(bannedWord) || descLower.contains(bannedWord)) {
                return new ModerationResult(ModerationStatus.REJECTED,
                        INAPPROPRIATE_LANGUAGE_MSG + bannedWord);
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
                    URLS_OR_CONTACTS_NOT_ALLOWED_MSG);
        }

        if (imageUrl != null && !isValidImageUrl(imageUrl)) {
            return new ModerationResult(ModerationStatus.REJECTED,
                    INVALID_IMAGE_URL_MSG);
        }

        if (price < MIN_NORMAL_PRICE || price >MAX_NORMAL_PRICE) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    PRICE_OUT_OF_RANGE_MSG);
        }

        if (stock > MAX_NORMAL_STOCK) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    HIGH_STOCK_REVIEW_MSG);
        }

        if (hasSuspicious) {
            return new ModerationResult(ModerationStatus.PENDING_REVIEW,
                    MANUAL_REVIEW_REQUIRED_MSG);
        }

        return new ModerationResult(ModerationStatus.APPROVED, AUTO_APPROVED_MSG);
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