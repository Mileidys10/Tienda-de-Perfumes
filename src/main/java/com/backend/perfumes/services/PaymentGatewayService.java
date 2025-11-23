package com.backend.perfumes.services;

import com.backend.perfumes.dto.PaymentResponseDTO;
import com.backend.perfumes.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PaymentGatewayService {

    @Value("${app.frontend.url:http://localhost:8100}")
    private String frontendUrl;

    private final Map<String, MockPayment> mockPayments = new ConcurrentHashMap<>();

    public PaymentResponseDTO createPayment(Order order, String paymentMethod) {
        try {
            String paymentId = "pi_mock_" + UUID.randomUUID().toString().substring(0, 8);

            MockPayment mockPayment = new MockPayment();
            mockPayment.setId(paymentId);
            mockPayment.setOrderId(order.getId());
            mockPayment.setOrderNumber(order.getOrderNumber());
            mockPayment.setAmount(order.getTotal());
            mockPayment.setStatus("requires_payment_method");
            mockPayment.setPaymentMethod(paymentMethod);
            mockPayment.setCreatedAt(System.currentTimeMillis());

            mockPayments.put(paymentId, mockPayment);

            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(paymentId);
            response.setStatus("requires_payment_method");
            response.setClientSecret("cs_mock_" + UUID.randomUUID().toString().substring(0, 8));
            response.setGatewayUrl(buildGatewayUrl(paymentId));
            response.setPaymentUrl(buildPaymentUrl(paymentId));

            log.info("Payment simulado creado para orden {}: {}", order.getOrderNumber(), paymentId);
            return response;

        } catch (Exception e) {
            log.error("Error creando pago simulado para orden {}", order.getOrderNumber(), e);
            throw new RuntimeException("Error al crear el pago: " + e.getMessage());
        }
    }

    private String buildGatewayUrl(String paymentIntentId) {
        return frontendUrl + "/payment?payment_intent=" + paymentIntentId;
    }

    private String buildPaymentUrl(String paymentIntentId) {
        return "/api/payments/simulate-payment?payment_id=" + paymentIntentId;
    }

    public boolean verifyPayment(String paymentIntentId) {
        MockPayment payment = mockPayments.get(paymentIntentId);
        return payment != null && "succeeded".equals(payment.getStatus());
    }

    public boolean simulatePayment(String paymentIntentId, boolean success) {
        try {
            MockPayment payment = mockPayments.get(paymentIntentId);
            if (payment == null) {
                return false;
            }

            if (success) {
                payment.setStatus("succeeded");
                payment.setPaidAt(System.currentTimeMillis());
                log.info("Pago simulado EXITOSO para: {}", paymentIntentId);
            } else {
                payment.setStatus("failed");
                log.info("Pago simulado FALLIDO para: {}", paymentIntentId);
            }

            mockPayments.put(paymentIntentId, payment);
            return true;

        } catch (Exception e) {
            log.error("Error simulando pago: {}", paymentIntentId, e);
            return false;
        }
    }

    public MockPayment getPayment(String paymentIntentId) {
        return mockPayments.get(paymentIntentId);
    }

    public static class MockPayment {
        private String id;
        private Long orderId;
        private String orderNumber;
        private BigDecimal amount;
        private String status;
        private String paymentMethod;
        private Long createdAt;
        private Long paidAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
        public Long getPaidAt() { return paidAt; }
        public void setPaidAt(Long paidAt) { this.paidAt = paidAt; }
    }
}