package com.backend.perfumes.services;

import com.backend.perfumes.dto.*;
import com.backend.perfumes.model.*;
import com.backend.perfumes.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PerfumeRepository perfumeRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayService paymentGatewayService;

    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.16); // 16% IVA
    private static final BigDecimal SHIPPING_COST = BigDecimal.valueOf(5.00);

    @Transactional
    public OrderResponseDTO createOrder(CheckoutRequestDTO checkoutRequest, String username) {
        log.info("Creando orden para usuario: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        OrderCalculationResult calculation = calculateOrderTotals(checkoutRequest.getItems());

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setSubtotal(calculation.getSubtotal());
        order.setTax(calculation.getTax());
        order.setShipping(calculation.getShipping());
        order.setTotal(calculation.getTotal());
        order.setShippingAddress(checkoutRequest.getShippingAddress());
        order.setBillingAddress(checkoutRequest.getBillingAddress());
        order.setCustomerEmail(checkoutRequest.getCustomerEmail());
        order.setCustomerPhone(checkoutRequest.getCustomerPhone());
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);
        log.info("Orden creada con ID: {}", savedOrder.getId());

        createOrderItems(savedOrder, calculation.getItems());

        PaymentResponseDTO paymentResponse = paymentGatewayService.createPayment(
                savedOrder, checkoutRequest.getPaymentMethod());

        createPaymentRecord(savedOrder, paymentResponse, checkoutRequest.getPaymentMethod());

        return buildOrderResponse(savedOrder, paymentResponse);
    }

    private OrderCalculationResult calculateOrderTotals(List<CartItemDTO> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItemCalculation> calculatedItems = new ArrayList<>();

        for (CartItemDTO cartItem : cartItems) {
            Perfume perfume = perfumeRepository.findById(cartItem.getPerfumeId())
                    .orElseThrow(() -> new RuntimeException("Perfume no encontrado: " + cartItem.getPerfumeId()));

            if (perfume.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para: " + perfume.getName() +
                        ". Stock disponible: " + perfume.getStock());
            }

            if (cartItem.getQuantity() <= 0) {
                throw new RuntimeException("Cantidad inválida para: " + perfume.getName());
            }

            BigDecimal itemTotal = perfume.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            calculatedItems.add(new OrderItemCalculation(perfume, cartItem.getQuantity(), itemTotal));
        }

        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal shipping = SHIPPING_COST;
        BigDecimal total = subtotal.add(tax).add(shipping);

        return new OrderCalculationResult(subtotal, tax, shipping, total, calculatedItems);
    }

    private void createOrderItems(Order order, List<OrderItemCalculation> calculatedItems) {
        for (OrderItemCalculation calc : calculatedItems) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setPerfume(calc.getPerfume());
            item.setQuantity(calc.getQuantity());
            item.setUnitPrice(BigDecimal.valueOf(calc.getPerfume().getPrice()));
            item.setTotalPrice(calc.getTotalPrice());

            orderItemRepository.save(item);

            // Actualizar stock inmediatamente
            Perfume perfume = calc.getPerfume();
            perfume.setStock(perfume.getStock() - calc.getQuantity());
            perfumeRepository.save(perfume);

            log.info("Stock actualizado para perfume {}: nuevo stock = {}",
                    perfume.getName(), perfume.getStock());
        }
    }

    private void createPaymentRecord(Order order, PaymentResponseDTO paymentResponse, String paymentMethod) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentGatewayId(paymentResponse.getPaymentId());
        payment.setAmount(order.getTotal());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        log.info("Registro de pago creado para orden: {}", order.getOrderNumber());
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() +
                "-" + System.currentTimeMillis() % 10000;
    }

    private OrderResponseDTO buildOrderResponse(Order order, PaymentResponseDTO paymentResponse) {
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus().toString());
        response.setSubtotal(order.getSubtotal());
        response.setTax(order.getTax());
        response.setShipping(order.getShipping());
        response.setTotal(order.getTotal());
        response.setPaymentUrl(paymentResponse.getGatewayUrl());
        response.setClientSecret(paymentResponse.getClientSecret());

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(this::convertToOrderItemResponseDTO)
                .collect(Collectors.toList());
        response.setItems(itemDTOs);

        response.setCreatedAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return response;
    }

    private OrderItemResponseDTO convertToOrderItemResponseDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setPerfumeId(item.getPerfume().getId());
        dto.setPerfumeName(item.getPerfume().getName());
        dto.setBrandName(item.getPerfume().getBrand().getName());
        dto.setImageUrl(item.getPerfume().getImageUrl());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }

    public Page<OrderResponseDTO> getUserOrders(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Page<Order> orders = orderRepository.findByUsername(username, pageable);

        List<OrderResponseDTO> orderDTOs = orders.getContent().stream()
                .map(order -> {
                    OrderResponseDTO dto = new OrderResponseDTO();
                    dto.setOrderId(order.getId());
                    dto.setOrderNumber(order.getOrderNumber());
                    dto.setStatus(order.getStatus().toString());
                    dto.setSubtotal(order.getSubtotal());
                    dto.setTax(order.getTax());
                    dto.setShipping(order.getShipping());
                    dto.setTotal(order.getTotal());
                    dto.setCreatedAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                    List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                            .map(this::convertToOrderItemResponseDTO)
                            .collect(Collectors.toList());
                    dto.setItems(itemDTOs);

                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(orderDTOs, pageable, orders.getTotalElements());
    }

    public OrderResponseDTO getOrderByNumber(String orderNumber, String username) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("No tienes permisos para ver esta orden");
        }

        return buildOrderResponse(order, new PaymentResponseDTO());
    }

    @Transactional
    public void cancelOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("No tienes permisos para cancelar esta orden");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Solo se pueden cancelar órdenes pendientes");
        }

        // Revertir stock
        for (OrderItem item : order.getItems()) {
            Perfume perfume = item.getPerfume();
            perfume.setStock(perfume.getStock() + item.getQuantity());
            perfumeRepository.save(perfume);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Cancelar pago si existe
        paymentRepository.findByOrder(order).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        });

        log.info("Orden {} cancelada por usuario {}", order.getOrderNumber(), username);
    }
}