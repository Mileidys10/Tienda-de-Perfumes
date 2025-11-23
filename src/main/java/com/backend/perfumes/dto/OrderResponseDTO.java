package com.backend.perfumes.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderResponseDTO {
    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal total;
    private String paymentUrl;
    private String clientSecret;
    private List<OrderItemResponseDTO> items;
    private String createdAt;

    public OrderResponseDTO() {}
}