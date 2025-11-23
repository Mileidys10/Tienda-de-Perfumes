package com.backend.perfumes.controller;

import com.backend.perfumes.dto.CheckoutRequestDTO;
import com.backend.perfumes.dto.OrderResponseDTO;
import com.backend.perfumes.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Gestión de órdenes y pagos")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear orden y proceso de pago")
    public ResponseEntity<?> checkout(
            @Valid @RequestBody CheckoutRequestDTO checkoutRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Solicitud de checkout recibida de usuario: {}", userDetails.getUsername());

        try {
            OrderResponseDTO orderResponse = orderService.createOrder(checkoutRequest, userDetails.getUsername());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Orden creada exitosamente");
            response.put("data", orderResponse);
            response.put("timestamp", LocalDateTime.now());

            log.info("Checkout completado para usuario: {}. Orden: {}",
                    userDetails.getUsername(), orderResponse.getOrderNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en checkout para usuario: {}", userDetails.getUsername(), e);

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener historial de órdenes del usuario")
    public ResponseEntity<?> getMyOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OrderResponseDTO> orders = orderService.getUserOrders(userDetails.getUsername(), pageable);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", orders.getContent());
            response.put("meta", Map.of(
                    "total", orders.getTotalElements(),
                    "page", orders.getNumber(),
                    "size", orders.getSize(),
                    "totalPages", orders.getTotalPages()
            ));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/{orderNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener detalles de una orden específica")
    public ResponseEntity<?> getOrderDetails(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            OrderResponseDTO order = orderService.getOrderByNumber(orderNumber, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", order,
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

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancelar una orden pendiente")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            orderService.cancelOrder(orderId, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Orden cancelada exitosamente",
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

    @PostMapping("/{orderNumber}/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirmar pago exitoso (para webhooks o callbacks)")
    public ResponseEntity<?> confirmPayment(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Esta endpoint sería llamado por el frontend después de un pago exitoso
            // o por un webhook de Stripe

            log.info("Confirmación de pago recibida para orden: {}", orderNumber);

            // Aquí iría la lógica para confirmar el pago y actualizar el estado de la orden
            // Por ahora retornamos éxito

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Pago confirmado exitosamente para orden: " + orderNumber,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error confirmando pago para orden: {}", orderNumber, e);

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Verificar estado del servicio de órdenes")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "service", "order-service",
                "timestamp", LocalDateTime.now(),
                "message", "Order service is running"
        ));
    }
}