package com.backend.perfumes.dto;

import com.backend.perfumes.model.Genre;
import com.backend.perfumes.model.ModerationStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PerfumeDTO {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 1000, message = "La descripción debe tener entre 10 y 1000 caracteres")
    private String description;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "10000.00", message = "El precio no puede exceder $10,000")
    private Double price;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El tamaño es obligatorio")
    @Min(value = 1, message = "El tamaño debe ser al menos 1ml")
    @Max(value = 1000, message = "El tamaño no puede exceder 1000ml")
    private Integer sizeMl;

    @NotNull(message = "El género es obligatorio")
    private Genre genre;

    private LocalDate releaseDate;

    @NotNull(message = "La marca es obligatoria")
    private Long brandId;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    private String imageUrl;

    private String creador;
    private String brandName;
    private String categoryName;
    private ModerationStatus moderationStatus;
    private String rejectionReason;
    private LocalDate createdAt;
}