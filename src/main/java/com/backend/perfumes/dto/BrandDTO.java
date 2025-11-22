package com.backend.perfumes.dto;

import com.backend.perfumes.model.ModerationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class BrandDTO {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
    private String description;

    @NotBlank(message = "El país de origen es obligatorio")
    private String countryOrigin;

    private String imageUrl;

    private String creador;
    private List<PerfumeDTO> perfumes;
    private Integer totalPerfumes;
    private ModerationStatus moderationStatus;
    private String rejectionReason;
}