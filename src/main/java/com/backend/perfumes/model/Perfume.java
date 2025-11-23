package com.backend.perfumes.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "perfumes")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Perfume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    private String description;

    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "10000.00", message = "El precio no puede exceder $10,000")
    private Double price;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @Min(value = 1, message = "El tamaño debe ser al menos 1ml")
    @Max(value = 1000, message = "El tamaño no puede exceder 1000ml")
    private Integer sizeMl;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    private LocalDate releaseDate;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.DRAFT;

    private String rejectionReason;
    private LocalDateTime moderationDate;
    private String moderatedBy = "AUTO_MODERATOR";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean featured = false;
    private int reportCount = 0;
    private int salesCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}