package com.backend.perfumes.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal total;
    private String paymentUrl; // URL para redirigir al pago
    private List<OrderItemDTO> items;
}