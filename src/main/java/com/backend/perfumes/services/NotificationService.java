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
public class  NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private static final String USER_NOT_FOUND = "Usuario no encontrado";
    private static final String TITLE_NEW_ORDER = "¡Nueva Venta! 🎉";

    private static final String TITLE_ORDER_UPDATE = "📦 Actualización de Orden";

    private static final String TITLE_LOW_STOCK = "⚠️ Stock Bajo";

    private static final String TITLE_PAYMENT_SUCCESS = "✅ Pago Exitoso";

    private static final String TITLE_PAYMENT_CONFIRMED = "💰 Pago Confirmado";
    private static final String MESSAGE_NEW_ORDER =
            "Tienes una nueva venta en la orden #%s. Productos: %s. Total: $%.2f";

    private static final String MESSAGE_ORDER_UPDATED =
            "Tu orden #%s ha sido actualizada a: %s";

    private static final String MESSAGE_LOW_STOCK =
            "El perfume '%s' tiene stock bajo. Stock actual: %d unidades";

    private static final String MESSAGE_PAYMENT_SUCCESS =
            "¡Felicidades! Tu pago para la orden #%s ha sido procesado exitosamente. Total: $%.2f";

    private static final String MESSAGE_PAYMENT_CONFIRMED =
            "El pago de la orden #%s ha sido confirmado. Tu ganancia: $%.2f";
    private static final String ERROR_MARK_READ =
            "Error al marcar notificaciones como leídas: ";

    private static final String ERROR_MARK_NOTIFICATION =
            "Error al marcar notificación como leída: ";

    private static final String NOTIFICATION_NOT_FOUND =
            "Notificación no encontrada o sin permisos";
    private static final long NOTIFICATION_RETENTION_DAYS = 30;
    




    private User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));
    }

    private Set<User> getOrderSellers(Order order) {
       return order.getItems().stream()
                .map(item -> item.getPerfume().getUser())
                .collect(Collectors.toSet());

    }

    private List<OrderItem> getSellerItems(Order order, User seller) {
        return order.getItems().stream()
                .filter(item -> item.getPerfume().getUser().getId().equals(seller.getId()))
                .collect(Collectors.toList());


    }

    private double calculateSellerTotal(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(item -> item.getTotalPrice().doubleValue())
                .sum();
    }

    private Notification buildNotification(
            User user,
            Order order,
            String title,
            String message,
            NotificationType type
    ) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setOrder(order);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notification;
    }


    @Transactional
    public void notifySellerNewOrder(Order order) {
        try {
            // Obtener todos los vendedores únicos de los productos en la orden
            Set<User> sellers = getOrderSellers(order);

            for (User seller : sellers) {
                // Filtrar items de este vendedor específico
                List<OrderItem> sellerItems = getSellerItems(order, seller);

                String productNames = sellerItems.stream()
                        .map(item -> item.getPerfume().getName())
                        .collect(Collectors.joining(", "));

                double totalVenta = calculateSellerTotal(sellerItems);

                Notification notification = new Notification();
                notification.setTitle("¡Nueva Venta! 🎉");
                notification.setMessage(String.format(
                        "Tienes una nueva venta en la orden #%s. Productos: %s. Total: $%.2f",
                        order.getOrderNumber(),
                        productNames,
                        totalVenta
                ));


                notificationRepository.save(buildNotification(seller, order, notification.getTitle(), notification.getMessage(), NotificationType.NEW_ORDER));

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
            User user = findByUsername(username);

            Notification notification = new Notification();
            notification.setTitle("📦 Actualización de Orden");
            notification.setMessage(String.format(
                    "Tu orden #%s ha sido actualizada a: %s",
                    order.getOrderNumber(),
                    order.getStatus().toString()
            ));


            notificationRepository.save(buildNotification(user, order, notification.getTitle(), notification.getMessage(), NotificationType.ORDER_UPDATE));

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
            Set<User> sellers = getOrderSellers(order);

            for (User seller : sellers) {
                List<OrderItem> sellerItems = getSellerItems(order, seller);

                double totalVenta = calculateSellerTotal(sellerItems);

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
         User user = findByUsername(username);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Notification> getUnreadNotifications(String username) {
        User user = findByUsername(username);
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(String username) {
        User user = findByUsername(username);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAllAsRead(String username) {
        try {
            User user = findByUsername(username);

            // Usar el método del repository
            int updatedCount = notificationRepository.markAllAsReadByUser(user);

            log.info("✅ {} notificaciones marcadas como leídas para: {}", updatedCount, username);

        } catch (Exception e) {
            log.error("❌ Error marcando todas las notificaciones como leídas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al marcar notificaciones como leídas: " + e.getMessage());
        }
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        try {
            User user = findByUsername(username);

            // Usar el método del repository
            int updated = notificationRepository.markAsRead(notificationId, user);
            if (updated == 0) {
                throw new RuntimeException("Notificación no encontrada o sin permisos");
            }

            log.info("✅ Notificación {} marcada como leída para: {}", notificationId, username);

        } catch (Exception e) {
            log.error("❌ Error marcando notificación como leída: {}", e.getMessage(), e);
            throw new RuntimeException("Error al marcar notificación como leída: " + e.getMessage());
        }
    }

    // Método adicional para obtener notificaciones recientes
    public List<Notification> getRecentNotifications(String username, int limit) {
        User user = findByUsername(username);

        List<Notification> notifications = notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        return notifications.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional
    public void cleanupOldNotificationsAlternative(String username) {
        User user = findByUsername(username);

        // ✅ CORREGIDO: Pasar el ID del usuario en lugar del objeto User
        notificationRepository.deleteOldNotificationsAlternative(user.getId());

        log.info("🧹 Notificaciones antiguas eliminadas (método alternativo) para: {}", username);
    }

    @Transactional
    public void cleanupOldNotifications(String username) {
        User user = findByUsername(username);

        // Calcular la fecha límite (30 días atrás)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        // Usar el método corregido
        notificationRepository.deleteOldNotifications(user, cutoffDate);

        log.info("🧹 Notificaciones antiguas eliminadas para: {}", username);
    }
}