package com.backend.perfumes.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long perfumeId;
    private String perfumeName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    // Constructor vacío
    public OrderItemDTO() {}

    // Constructor con parámetros
    public OrderItemDTO(Long perfumeId, String perfumeName, String imageUrl,
                        Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        this.perfumeId = perfumeId;
        this.perfumeName = perfumeName;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }
}