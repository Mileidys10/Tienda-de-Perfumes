package com.backend.perfumes.dto;

import lombok.Data;
import java.util.List;

@Data
public class BrandDTO {
    private Long id;
    private String name;
    private String description;
    private String countryOrigin;
    private String creador;
    private List<PerfumeDTO> perfumes;
    private Integer totalPerfumes;
    private String imageUrl;


}