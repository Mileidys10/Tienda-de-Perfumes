package com.backend.perfumes.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartItemDTO {
    private Long perfumeId;
    private Integer quantity;
}