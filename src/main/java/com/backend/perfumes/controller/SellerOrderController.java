package com.backend.perfumes.controller;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderStatus;
import com.backend.perfumes.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seller Orders", description = "Gestión de órdenes para vendedores")
public class SellerOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    @Operation(summary = "Obtener órdenes del vendedor")
    public ResponseEntity<?> getSellerOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orders = orderService.getSellerOrders(userDetails.getUsername(), pageable, status);

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

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    @Operation(summary = "Obtener detalles de una orden específica para el vendedor")
    public ResponseEntity<?> getSellerOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Order order = orderService.getSellerOrderDetail(orderId, userDetails.getUsername());

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

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    @Operation(summary = "Actualizar estado de una orden")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String statusStr = request.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "El campo 'status' es requerido",
                        "timestamp", LocalDateTime.now()
                ));
            }

            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Estado de orden actualizado exitosamente",
                    "data", Map.of(
                            "orderId", updatedOrder.getId(),
                            "orderNumber", updatedOrder.getOrderNumber(),
                            "newStatus", updatedOrder.getStatus()
                    ),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Estado de orden inválido",
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