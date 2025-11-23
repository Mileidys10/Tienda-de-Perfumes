package com.backend.perfumes.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MockPayment {
    private String id;
    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private Long createdAt;
    private Long paidAt;
}