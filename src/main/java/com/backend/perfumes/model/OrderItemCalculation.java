package com.backend.perfumes.model;

import java.math.BigDecimal;

public class OrderItemCalculation {
    private final Perfume perfume;
    private final Integer quantity;
    private final BigDecimal totalPrice;

    public OrderItemCalculation(Perfume perfume, Integer quantity, BigDecimal totalPrice) {
        this.perfume = perfume;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public Perfume getPerfume() { return perfume; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
}