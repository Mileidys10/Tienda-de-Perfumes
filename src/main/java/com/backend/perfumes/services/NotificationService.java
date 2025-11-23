// NotificationService.java
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void notifySellerNewOrder(Order order) {
        try {
            User seller = order.getItems().get(0).getPerfume().getUser();

            Notification notification = new Notification();
            notification.setUser(seller);
            notification.setTitle("Nueva Orden Recibida");
            notification.setMessage("Tienes una nueva orden #" + order.getOrderNumber() + " para preparar");
            notification.setType(NotificationType.NEW_ORDER);
            notification.setOrder(order);

            notificationRepository.save(notification);
            log.info("Notificación de nueva orden enviada al vendedor: {}", seller.getUsername());

        } catch (Exception e) {
            log.error("Error enviando notificación de nueva orden: {}", e.getMessage());
        }
    }

    public void notifyOrderStatusUpdate(Order order, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle("Actualización de Orden");
            notification.setMessage("Tu orden #" + order.getOrderNumber() + " ha sido actualizada a: " + order.getStatus());
            notification.setType(NotificationType.ORDER_UPDATE);
            notification.setOrder(order);

            notificationRepository.save(notification);
            log.info("Notificación de actualización de orden enviada al usuario: {}", username);

        } catch (Exception e) {
            log.error("Error enviando notificación de actualización de orden: {}", e.getMessage());
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

        notificationRepository.markAllAsReadByUser(user);
        log.info("Todas las notificaciones marcadas como leídas para: {}", username);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        notificationRepository.markAsRead(notificationId, user);
        log.info("Notificación {} marcada como leída para: {}", notificationId, username);
    }
}