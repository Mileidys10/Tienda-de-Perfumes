package com.backend.perfumes.controller;

import com.backend.perfumes.model.Notification;
import com.backend.perfumes.services.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Gestión de notificaciones")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener notificaciones del usuario")
    public ResponseEntity<?> getNotifications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notificationsPage = notificationService.getUserNotifications(userDetails.getUsername(), pageable);

            // Convertir a DTO usando getters
            Page<Map<String, Object>> notifications = notificationsPage.map(notification -> {
                Map<String, Object> notifMap = new LinkedHashMap<>();
                notifMap.put("id", notification.getId());
                notifMap.put("title", notification.getTitle());
                notifMap.put("message", notification.getMessage());
                notifMap.put("type", notification.getType());
                notifMap.put("isRead", notification.getIsRead()); // Usar getter
                notifMap.put("createdAt", notification.getCreatedAt());
                if (notification.getOrder() != null) {
                    notifMap.put("orderNumber", notification.getOrder().getOrderNumber());
                }
                return notifMap;
            });

            long unreadCount = notificationService.getUnreadCount(userDetails.getUsername());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", notifications.getContent());
            response.put("meta", Map.of(
                    "total", notifications.getTotalElements(),
                    "page", notifications.getNumber(),
                    "size", notifications.getSize(),
                    "totalPages", notifications.getTotalPages(),
                    "unreadCount", unreadCount
            ));
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar todas las notificaciones como leídas")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            notificationService.markAllAsRead(userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Todas las notificaciones marcadas como leídas",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error marcando todas las notificaciones como leídas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @PostMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar una notificación específica como leída")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            notificationService.markAsRead(id, userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Notificación marcada como leída",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error marcando notificación como leída: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener conteo de notificaciones no leídas")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            long unreadCount = notificationService.getUnreadCount(userDetails.getUsername());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "unreadCount", unreadCount,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo conteo de notificaciones no leídas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener notificaciones recientes")
    public ResponseEntity<?> getRecentNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            var recentNotifications = notificationService.getRecentNotifications(userDetails.getUsername(), 5)
                    .stream()
                    .map(notification -> {
                        Map<String, Object> notifMap = new LinkedHashMap<>();
                        notifMap.put("id", notification.getId());
                        notifMap.put("title", notification.getTitle());
                        notifMap.put("message", notification.getMessage());
                        notifMap.put("type", notification.getType());
                        notifMap.put("isRead", notification.getIsRead()); // Usar getter
                        notifMap.put("createdAt", notification.getCreatedAt());
                        if (notification.getOrder() != null) {
                            notifMap.put("orderNumber", notification.getOrder().getOrderNumber());
                        }
                        return notifMap;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", recentNotifications,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error obteniendo notificaciones recientes: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }
}