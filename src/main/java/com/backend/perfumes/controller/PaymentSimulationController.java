package com.backend.perfumes.controller;

import com.backend.perfumes.services.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Simulación de pagos para testing")
public class PaymentSimulationController {

    private final PaymentGatewayService paymentGatewayService;

    @GetMapping("/simulate-payment")
    @Operation(summary = "Simular proceso de pago (para testing)")
    public ResponseEntity<?> simulatePayment(
            @RequestParam String payment_id,
            @RequestParam(defaultValue = "true") boolean success) {

        try {
            boolean result = paymentGatewayService.simulatePayment(payment_id, success);

            if (!result) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Pago no encontrado",
                        "timestamp", LocalDateTime.now()
                ));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", success ?
                    "Pago simulado exitosamente" : "Pago simulado fallido");
            response.put("paymentId", payment_id);
            response.put("success", success);
            response.put("timestamp", LocalDateTime.now());

            if (success) {
                response.put("redirectUrl", "/api/orders/confirm-payment?payment_id=" + payment_id);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
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
                        "message", "payment_id es requerido",
                        "timestamp", LocalDateTime.now()
                ));
            }

            boolean paymentVerified = paymentGatewayService.verifyPayment(paymentId);

            if (paymentVerified) {

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Pago confirmado exitosamente",
                        "paymentId", paymentId,
                        "timestamp", LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Pago no verificado o fallido",
                        "timestamp", LocalDateTime.now()
                ));
            }

        } catch (Exception e) {
            log.error("Error confirmando pago: {}", e.getMessage());
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
                        "message", "Pago no encontrado",
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
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}