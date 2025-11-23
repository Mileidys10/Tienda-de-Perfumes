package com.backend.perfumes.dto;

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {
    private List<CartItemDTO> items;
    private String shippingAddress;
    private String billingAddress;
    private String customerEmail;
    private String customerPhone;
    private String paymentMethod;
}
