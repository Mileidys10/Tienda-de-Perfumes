package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.Payment;
import com.backend.perfumes.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByPaymentGatewayId(String paymentGatewayId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByOrderOrderByCreatedAtDesc(Order order);
}