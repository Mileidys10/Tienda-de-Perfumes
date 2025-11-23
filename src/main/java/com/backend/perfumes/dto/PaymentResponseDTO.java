package com.backend.perfumes.dto;

import lombok.Data;

@Data
public class PaymentResponseDTO {
    private String paymentId;
    private String status;
    private String gatewayUrl;
    private String clientSecret; // Para Stripe
}