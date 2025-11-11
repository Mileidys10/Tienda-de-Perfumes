package com.backend.perfumes.dto;

import com.backend.perfumes.model.Genre;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PerfumeDTO {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
    private String description;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El tamaño es obligatorio")
    @Min(value = 1, message = "El tamaño debe ser al menos 1 ml")
    private Integer sizeMl;

    @NotNull(message = "El género es obligatorio")
    private Genre genre;

    @NotNull(message = "La fecha de lanzamiento es obligatoria")
    private LocalDate releaseDate;

    @NotNull(message = "La marca es obligatoria")
    private Long brandId;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    private String creador;
}