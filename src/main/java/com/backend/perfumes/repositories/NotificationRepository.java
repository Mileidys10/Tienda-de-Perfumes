package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Notification;
import com.backend.perfumes.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    List<Notification> findByUserAndIsReadFalse(User user);

    Optional<Notification> findByIdAndUser(Long id, User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    int markAsRead(@Param("id") Long id, @Param("user") User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC")
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(@Param("user") User user);

    // ✅ CORREGIDO: Usar LocalDateTime para la fecha límite
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("user") User user, @Param("cutoffDate") LocalDateTime cutoffDate);

    // ✅ CORREGIDO: Método alternativo usando función de fecha
    @Modifying
    @Query(value = "DELETE FROM notifications WHERE user_id = :userId AND created_at < NOW() - INTERVAL '30 days'", nativeQuery = true)
    void deleteOldNotificationsAlternative(@Param("userId") Long userId);
}