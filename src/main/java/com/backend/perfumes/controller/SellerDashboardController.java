package com.backend.perfumes.controller;


import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderStatus;
import com.backend.perfumes.services.NotificationService;
import com.backend.perfumes.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/seller")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seller Dashboard", description = "Dashboard y estadísticas para vendedores")
public class SellerDashboardController {

    private final OrderService orderService;
    private final NotificationService notificationService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    @Operation(summary = "Obtener estadísticas del dashboard del vendedor")
    public ResponseEntity<?> getSellerDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("📊 Obteniendo dashboard para vendedor: {}", userDetails.getUsername());

            // Obtener estadísticas de órdenes
            Map<String, Object> orderStats = orderService.getSellerStats(userDetails.getUsername());

            // Obtener conteo de notificaciones no leídas
            long unreadNotifications = notificationService.getUnreadCount(userDetails.getUsername());

            // Obtener órdenes recientes
            Pageable recentPageable = PageRequest.of(0, 5);
            Page<Order> recentOrders = orderService.getSellerOrders(userDetails.getUsername(), recentPageable, null);

            Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("orderStats", orderStats);
            dashboard.put("unreadNotifications", unreadNotifications);
            dashboard.put("recentOrders", recentOrders.getContent());
            dashboard.put("lastUpdated", LocalDateTime.now());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Dashboard obtenido exitosamente");
            response.put("data", dashboard);
            response.put("timestamp", LocalDateTime.now());

            log.info("✅ Dashboard generado para vendedor: {}", userDetails.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error obteniendo dashboard: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/recent-sales")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    @Operation(summary = "Obtener ventas recientes del vendedor")
    public ResponseEntity<?> getRecentSales(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> recentOrders = orderService.getSellerOrders(userDetails.getUsername(), pageable, null);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Ventas recientes obtenidas exitosamente");
            response.put("data", recentOrders.getContent());
            response.put("meta", Map.of(
                    "total", recentOrders.getTotalElements(),
                    "page", recentOrders.getNumber(),
                    "size", recentOrders.getSize(),
                    "totalPages", recentOrders.getTotalPages()
            ));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error obteniendo ventas recientes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/sales-stats")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    @Operation(summary = "Obtener estadísticas detalladas de ventas")
    public ResponseEntity<?> getSalesStats(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Map<String, Object> stats = orderService.getSellerStats(userDetails.getUsername());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Estadísticas obtenidas exitosamente");
            response.put("data", stats);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}