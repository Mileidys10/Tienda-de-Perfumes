package com.backend.perfumes.model;

import java.math.BigDecimal;
import java.util.List;

public class OrderCalculationResult {
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal shipping;
    private final BigDecimal total;
    private final List<OrderItemCalculation> items;

    public OrderCalculationResult(BigDecimal subtotal, BigDecimal tax, BigDecimal shipping,
                                  BigDecimal total, List<OrderItemCalculation> items) {
        this.subtotal = subtotal;
        this.tax = tax;
        this.shipping = shipping;
        this.total = total;
        this.items = items;
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getShipping() { return shipping; }
    public BigDecimal getTotal() { return total; }
    public List<OrderItemCalculation> getItems() { return items; }
}
