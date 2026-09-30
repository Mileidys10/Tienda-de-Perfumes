package com.backend.perfumes.services;

import com.backend.perfumes.dto.PaymentResponseDTO;
import com.backend.perfumes.model.MockPayment;
import com.backend.perfumes.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentGatewayServiceTest {

    private final PaymentGatewayService paymentGatewayService = new PaymentGatewayService();

    @Test
    @DisplayName("Crear pago mock genera ID valido y URL de pasarela")
    void testCreatePayment() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-PAY-001");
        order.setTotal(BigDecimal.valueOf(85.50));

        PaymentResponseDTO response = paymentGatewayService.createPayment(order, "CREDIT_CARD");

        assertNotNull(response);
        assertNotNull(response.getPaymentId());
        assertTrue(response.getPaymentId().startsWith("pi_mock_"));
        assertEquals("requires_payment_method", response.getStatus());
        assertNotNull(response.getGatewayUrl());
    }

    @Test
    @DisplayName("Simular pago mock actualiza estado y verifica correctamente")
    void testSimulateAndVerifyPayment() {
        Order order = new Order();
        order.setId(20L);
        order.setOrderNumber("ORD-PAY-002");
        order.setTotal(BigDecimal.valueOf(150.00));

        PaymentResponseDTO response = paymentGatewayService.createPayment(order, "PSE");
        String paymentId = response.getPaymentId();

        // Antes de simular debe ser no verificado
        assertFalse(paymentGatewayService.verifyPayment(paymentId));

        // Simular exito
        boolean simResult = paymentGatewayService.simulatePayment(paymentId, true);
        assertTrue(simResult);
        assertTrue(paymentGatewayService.verifyPayment(paymentId));
    }
}
