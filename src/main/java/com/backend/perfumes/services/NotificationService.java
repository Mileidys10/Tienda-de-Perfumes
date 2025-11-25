package com.backend.perfumes.services;

import com.backend.perfumes.model.*;
import com.backend.perfumes.repositories.NotificationRepository;
import com.backend.perfumes.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notifySellerNewOrder(Order order) {
        try {
            // Obtener todos los vendedores únicos de los productos en la orden
            Set<User> sellers = order.getItems().stream()
                    .map(item -> item.getPerfume().getUser())
                    .collect(Collectors.toSet());

            for (User seller : sellers) {
                // Filtrar items de este vendedor específico
                List<OrderItem> sellerItems = order.getItems().stream()
                        .filter(item -> item.getPerfume().getUser().getId().equals(seller.getId()))
                        .collect(Collectors.toList());

                String productNames = sellerItems.stream()
                        .map(item -> item.getPerfume().getName())
                        .collect(Collectors.joining(", "));

                double totalVenta = sellerItems.stream()
                        .mapToDouble(item -> item.getTotalPrice().doubleValue())
                        .sum();

                Notification notification = new Notification();
                notification.setTitle("¡Nueva Venta! 🎉");
                notification.setMessage(String.format(
                        "Tienes una nueva venta en la orden #%s. Productos: %s. Total: $%.2f",
                        order.getOrderNumber(),
                        productNames,
                        totalVenta
                ));
                notification.setType(NotificationType.NEW_ORDER);
                notification.setUser(seller);
                notification.setOrder(order);
                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());

                notificationRepository.save(notification);

                log.info("📦 Notificación de nueva venta enviada al vendedor: {} - Orden: {}",
                        seller.getUsername(), order.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("❌ Error enviando notificaciones de nueva orden: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void notifyOrderStatusUpdate(Order order, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Notification notification = new Notification();
            notification.setTitle("📦 Actualización de Orden");
            notification.setMessage(String.format(
                    "Tu orden #%s ha sido actualizada a: %s",
                    order.getOrderNumber(),
                    order.getStatus().toString()
            ));
            notification.setType(NotificationType.ORDER_UPDATE);
            notification.setUser(user);
            notification.setOrder(order);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);

            log.info("🔔 Notificación de actualización enviada a: {} - Orden: {}",
                    username, order.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Error enviando notificación de actualización: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void notifyLowStock(Perfume perfume) {
        try {
            User seller = perfume.getUser();

            Notification notification = new Notification();
            notification.setTitle("⚠️ Stock Bajo");
            notification.setMessage(String.format(
                    "El perfume '%s' tiene stock bajo. Stock actual: %d unidades",
                    perfume.getName(),
                    perfume.getStock()
            ));
            notification.setType(NotificationType.STOCK_ALERT);
            notification.setUser(seller);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);

            log.info("📉 Notificación de stock bajo enviada a: {} - Producto: {}",
                    seller.getUsername(), perfume.getName());
        } catch (Exception e) {
            log.error("❌ Error enviando notificación de stock bajo: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void notifyPaymentSuccess(Order order) {
        try {
            // Notificar al cliente
            Notification clientNotification = new Notification();
            clientNotification.setTitle("✅ Pago Exitoso");
            clientNotification.setMessage(String.format(
                    "¡Felicidades! Tu pago para la orden #%s ha sido procesado exitosamente. Total: $%.2f",
                    order.getOrderNumber(),
                    order.getTotal().doubleValue()
            ));
            clientNotification.setType(NotificationType.PAYMENT_SUCCESS);
            clientNotification.setUser(order.getUser());
            clientNotification.setOrder(order);
            clientNotification.setRead(false);
            clientNotification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(clientNotification);

            // Notificar a los vendedores
            Set<User> sellers = order.getItems().stream()
                    .map(item -> item.getPerfume().getUser())
                    .collect(Collectors.toSet());

            for (User seller : sellers) {
                List<OrderItem> sellerItems = order.getItems().stream()
                        .filter(item -> item.getPerfume().getUser().getId().equals(seller.getId()))
                        .collect(Collectors.toList());

                double totalVenta = sellerItems.stream()
                        .mapToDouble(item -> item.getTotalPrice().doubleValue())
                        .sum();

                Notification sellerNotification = new Notification();
                sellerNotification.setTitle("💰 Pago Confirmado");
                sellerNotification.setMessage(String.format(
                        "El pago de la orden #%s ha sido confirmado. Tu ganancia: $%.2f",
                        order.getOrderNumber(),
                        totalVenta
                ));
                sellerNotification.setType(NotificationType.PAYMENT_SUCCESS);
                sellerNotification.setUser(seller);
                sellerNotification.setOrder(order);
                sellerNotification.setRead(false);
                sellerNotification.setCreatedAt(LocalDateTime.now());

                notificationRepository.save(sellerNotification);
            }

            log.info("💳 Notificaciones de pago exitoso enviadas para orden: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("❌ Error enviando notificaciones de pago exitoso: {}", e.getMessage(), e);
        }
    }

    public Page<Notification> getUserNotifications(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Notification> getUnreadNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Opción 1: Usando el método del repository (más eficiente)
        notificationRepository.markAllAsReadByUser(user);

        log.info("✅ Todas las notificaciones marcadas como leídas para: {}", username);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Opción 1: Usando el método del repository (más eficiente)
        int updated = notificationRepository.markAsRead(notificationId, user);
        if (updated == 0) {
            throw new RuntimeException("Notificación no encontrada o sin permisos");
        }

        log.info("✅ Notificación {} marcada como leída para: {}", notificationId, username);
    }

    // Método adicional para obtener notificaciones recientes
    public List<Notification> getRecentNotifications(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Notification> notifications = notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        return notifications.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional
    public void cleanupOldNotificationsAlternative(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ CORREGIDO: Pasar el ID del usuario en lugar del objeto User
        notificationRepository.deleteOldNotificationsAlternative(user.getId());

        log.info("🧹 Notificaciones antiguas eliminadas (método alternativo) para: {}", username);
    }

    @Transactional
    public void cleanupOldNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Calcular la fecha límite (30 días atrás)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        // Usar el método corregido
        notificationRepository.deleteOldNotifications(user, cutoffDate);

        log.info("🧹 Notificaciones antiguas eliminadas para: {}", username);
    }


}