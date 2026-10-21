package com.backend.perfumes.controller;

import com.backend.perfumes.services.OrderService;
import com.backend.perfumes.services.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Simulacion de pagos para testing")
public class PaymentSimulationController {

    private final PaymentGatewayService paymentGatewayService;
    private final OrderService orderService;

    private static final String PAYMENT_INTENT_AND_SUCCESS_REQUIRED = "paymentIntentId y success son requeridos";
    private static final String PAYMENT_NOT_FOUND_MSG = "Pago no encontrado";
    private static final String PAYMENT_ID_REQUIRED_MSG = "payment_id es requerido";
    private static final String PAYMENT_CONFIRMED_SUCCESS_MSG = "Pago confirmado exitosamente";
    private static final String PAYMENT_FAILED_MSG = "Pago no verificado o fallido";

    @PostMapping("/simulate-payment")
    @Operation(summary = "Simular pago para testing")
    public ResponseEntity<?> simulatePayment(@RequestBody Map<String, Object> request) {
        try {
            String paymentIntentId = (String) request.get("paymentIntentId");
            Boolean success = (Boolean) request.get("success");

            if (paymentIntentId == null || success == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", PAYMENT_INTENT_AND_SUCCESS_REQUIRED,
                        "timestamp", LocalDateTime.now()
                ));
            }

            boolean simulated = paymentGatewayService.simulatePayment(paymentIntentId, success);

            if (!simulated) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", PAYMENT_NOT_FOUND_MSG,
                        "timestamp", LocalDateTime.now()
                ));
            }

            if (success) {
                try {
                    orderService.confirmPayment(paymentIntentId);
                    log.info("Pago confirmado exitosamente: {}", paymentIntentId);
                } catch (Exception e) {
                    log.error("Error confirmando pago {}: {}", paymentIntentId, e.getMessage(), e);
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Pago simulado pero error confirmando: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", success ? "Pago simulado y confirmado exitosamente" : "Pago simulado como fallido",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error simulando pago: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @PostMapping("/confirm-payment")
    @Operation(summary = "Confirmar pago exitoso")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, String> request) {
        try {
            String paymentId = request.get("payment_id");
            if (paymentId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", PAYMENT_ID_REQUIRED_MSG,
                        "timestamp", LocalDateTime.now()
                ));
            }

            boolean paymentVerified = paymentGatewayService.verifyPayment(paymentId);

            if (paymentVerified) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", PAYMENT_CONFIRMED_SUCCESS_MSG,
                        "paymentId", paymentId,
                        "timestamp", LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", PAYMENT_FAILED_MSG,
                        "timestamp", LocalDateTime.now()
                ));
            }

        } catch (Exception e) {
            log.error("Error confirmando pago: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/status/{paymentId}")
    @Operation(summary = "Verificar estado de un pago")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) {
        try {
            PaymentGatewayService.MockPayment payment = paymentGatewayService.getPayment(paymentId);

            if (payment == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", PAYMENT_NOT_FOUND_MSG,
                        "timestamp", LocalDateTime.now()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "payment", Map.of(
                            "id", payment.getId(),
                            "orderNumber", payment.getOrderNumber(),
                            "amount", payment.getAmount(),
                            "status", payment.getStatus(),
                            "paymentMethod", payment.getPaymentMethod()
                    ),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo estado de pago {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}
