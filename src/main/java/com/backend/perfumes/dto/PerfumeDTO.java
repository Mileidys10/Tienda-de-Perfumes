package com.backend.perfumes.dto;

import com.backend.perfumes.model.Genre;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PerfumeDTO {

    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String name;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 500, message = "La descripción no puede tener más de 500 caracteres")
    private String description;

    @Positive(message = "El precio debe ser mayor que 0")
    private float price;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private int stock;

    @Min(value = 1, message = "El tamaño debe ser al menos 1 ml")
    private int sizeMl;

    @NotNull(message = "El género es obligatorio")
    private Genre genre;

    @NotBlank(message = "La fecha de lanzamiento no puede estar vacía")
    private String releaseDate;

    @NotNull(message = "Debe especificar una marca válida (brandId)")
    private Long brandId;

    @NotNull(message = "Debe especificar una categoría válida (categoryId)")
    private Long categoryId;


}
