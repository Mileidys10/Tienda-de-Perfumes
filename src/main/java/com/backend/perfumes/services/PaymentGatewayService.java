package com.backend.perfumes.services;

import com.backend.perfumes.dto.PaymentResponseDTO;
import com.backend.perfumes.model.MockPayment;
import com.backend.perfumes.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PaymentGatewayService {

    @Value("${app.frontend.url:http://localhost:8100}")
    private String frontendUrl;

    // Constantes de estado de pasarela
    private static final String STATUS_REQUIRES_PAYMENT_METHOD = "requires_payment_method";
    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";
    private static final String PAYMENT_MOCK_PREFIX = "pi_mock_";
    private static final String SECRET_MOCK_PREFIX = "secret_mock_";
    private static final String CHECKOUT_SUCCESS_URL_FORMAT = "%s/checkout/success?payment_id=%s&order=%s";

    // Constantes de logs
    private static final String LOG_CREATING_PAYMENT = "Iniciando creación de pago mock para orden: {} - Monto: {}";
    private static final String LOG_PAYMENT_CREATED = "Pago mock creado exitosamente con ID: {}";
    private static final String LOG_VERIFYING_PAYMENT = "Verificando estado de pago ID: {}";
    private static final String LOG_SIMULATING_PAYMENT = "Simulando transacción para ID: {} - Resultado: {}";
    private static final String LOG_PAYMENT_NOT_FOUND = "Pago mock no encontrado para ID: {}";

    private final Map<String, MockPayment> mockPayments = new ConcurrentHashMap<>();

    public PaymentResponseDTO createPayment(Order order, String paymentMethod) {
        try {
            String paymentId = PAYMENT_MOCK_PREFIX + UUID.randomUUID().toString().substring(0, 8);
            log.info(LOG_CREATING_PAYMENT, order.getOrderNumber(), order.getTotal());

            MockPayment mockPayment = new MockPayment();
            mockPayment.setId(paymentId);
            mockPayment.setOrderId(order.getId());
            mockPayment.setOrderNumber(order.getOrderNumber());
            mockPayment.setAmount(order.getTotal());
            mockPayment.setStatus(STATUS_REQUIRES_PAYMENT_METHOD);
            mockPayment.setPaymentMethod(paymentMethod);

            mockPayments.put(paymentId, mockPayment);

            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(paymentId);
            response.setClientSecret(SECRET_MOCK_PREFIX + UUID.randomUUID().toString().substring(0, 16));
            response.setStatus(mockPayment.getStatus());
            response.setGatewayUrl(String.format(CHECKOUT_SUCCESS_URL_FORMAT, frontendUrl, paymentId, order.getOrderNumber()));

            log.info(LOG_PAYMENT_CREATED, paymentId);
            return response;

        } catch (Exception e) {
            log.error("Error creando pago mock para orden {}: {}", order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Error en gateway de pagos: " + e.getMessage());
        }
    }

    public boolean verifyPayment(String paymentId) {
        log.info(LOG_VERIFYING_PAYMENT, paymentId);

        MockPayment payment = mockPayments.get(paymentId);
        if (payment == null) {
            log.warn(LOG_PAYMENT_NOT_FOUND, paymentId);
            return false;
        }

        return STATUS_SUCCEEDED.equals(payment.getStatus());
    }

    public boolean simulatePayment(String paymentId, boolean success) {
        log.info(LOG_SIMULATING_PAYMENT, paymentId, success ? "APROBADO" : "RECHAZADO");

        MockPayment payment = mockPayments.get(paymentId);
        if (payment == null) {
            log.warn(LOG_PAYMENT_NOT_FOUND, paymentId);
            return false;
        }

        payment.setStatus(success ? STATUS_SUCCEEDED : STATUS_FAILED);
        return success;
    }

    public MockPayment getPayment(String paymentId) {
        return mockPayments.get(paymentId);
    }

    public static class MockPayment {
        private String id;
        private Long orderId;
        private String orderNumber;
        private java.math.BigDecimal amount;
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
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
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
