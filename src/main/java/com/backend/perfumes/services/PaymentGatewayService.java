package com.backend.perfumes.services;

import com.backend.perfumes.dto.PaymentResponseDTO;
import com.backend.perfumes.model.Order;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentGatewayService {

    @Value("${stripe.secret.key:sk_test_123}")
    private String stripeSecretKey;

    @Value("${app.frontend.url:http://localhost:8100}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe configurado con clave: {}", stripeSecretKey.substring(0, 8) + "...");
    }

    public PaymentResponseDTO createPayment(Order order, String paymentMethod) {
        try {
            // Para testing, si es modo demo, simular respuesta
            if (stripeSecretKey.equals("sk_test_123")) {
                return createMockPaymentResponse(order);
            }

            long amount = order.getTotal().multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("usd")
                    .setDescription("Orden #" + order.getOrderNumber())
                    .putMetadata("orderId", order.getId().toString())
                    .putMetadata("orderNumber", order.getOrderNumber())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            PaymentResponseDTO response = new PaymentResponseDTO();
            response.setPaymentId(paymentIntent.getId());
            response.setStatus(paymentIntent.getStatus());
            response.setClientSecret(paymentIntent.getClientSecret());
            response.setGatewayUrl(buildGatewayUrl(paymentIntent.getId()));

            log.info("PaymentIntent creado para orden {}: {}", order.getOrderNumber(), paymentIntent.getId());
            return response;

        } catch (StripeException e) {
            log.error("Error creando PaymentIntent para orden {}", order.getOrderNumber(), e);
            throw new RuntimeException("Error al crear el pago: " + e.getMessage());
        }
    }

    private PaymentResponseDTO createMockPaymentResponse(Order order) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentId("pi_mock_" + System.currentTimeMillis());
        response.setStatus("requires_payment_method");
        response.setClientSecret("cs_mock_" + System.currentTimeMillis());
        response.setGatewayUrl(frontendUrl + "/payment/success?order=" + order.getOrderNumber());

        log.info("Respuesta mock de pago creada para orden: {}", order.getOrderNumber());
        return response;
    }

    private String buildGatewayUrl(String paymentIntentId) {
        return frontendUrl + "/payment?payment_intent=" + paymentIntentId;
    }

    public boolean verifyPayment(String paymentIntentId) {
        try {
            if (paymentIntentId.startsWith("pi_mock_")) {
                return true; // En modo mock, siempre retorna éxito
            }

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(paymentIntent.getStatus());
        } catch (StripeException e) {
            log.error("Error verificando pago {}", paymentIntentId, e);
            return false;
        }
    }

    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        try {
            if (paymentIntentId.startsWith("pi_mock_")) {
                return null; // No hay PaymentIntent real en modo mock
            }

            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Error obteniendo PaymentIntent {}", paymentIntentId, e);
            throw new RuntimeException("Error obteniendo información del pago");
        }
    }
}