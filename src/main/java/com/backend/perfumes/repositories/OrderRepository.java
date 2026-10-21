package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderStatus;
import com.backend.perfumes.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserAndStatus(@Param("user") User user, @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT o FROM Order o WHERE o.user.username = :username ORDER BY o.createdAt DESC")
    Page<Order> findByUsername(@Param("username") String username, Pageable pageable);


    @Query("SELECT o FROM Order o JOIN o.items i WHERE i.perfume.user.id = :sellerId AND o.id = :orderId")
    Optional<Order> findBySellerAndOrderId(@Param("sellerId") Long sellerId, @Param("orderId") Long orderId);


    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.perfume.user.id = :sellerId")
    Page<Order> findBySeller(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.perfume.user.id = :sellerId AND o.status = :status")
    Page<Order> findBySellerAndStatus(@Param("sellerId") Long sellerId, @Param("status") OrderStatus status, Pageable pageable);


    @Query("SELECT COUNT(o) FROM Order o JOIN o.items oi WHERE oi.perfume.user.id = :sellerId")
    long countBySeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.items oi WHERE oi.perfume.user.id = :sellerId AND o.status = :status")
    long countBySellerAndStatus(@Param("sellerId") Long sellerId, @Param("status") OrderStatus status);

    @Query("SELECT SUM(oi.totalPrice) FROM Order o JOIN o.items oi WHERE oi.perfume.user.id = :sellerId AND o.status = 'DELIVERED'")
    Double getTotalRevenueBySeller(@Param("sellerId") Long sellerId);
}