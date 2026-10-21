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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PerfumeRepository perfumeRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final EmailService emailService;

    // Constantes de negocio
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.16); // 16% IVA
    private static final BigDecimal SHIPPING_COST = BigDecimal.valueOf(5.00);
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final String ORDER_NUMBER_PREFIX = "ORD-";

    // Constantes de mensajes de error de dominio
    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado";
    private static final String ORDER_NOT_FOUND_MSG = "Orden no encontrada";
    private static final String PAYMENT_NOT_FOUND_MSG = "Pago no encontrado";
    private static final String PERFUME_NOT_FOUND_PREFIX_MSG = "Perfume no encontrado: ";
    private static final String EMPTY_CART_MSG = "El carrito está vacío";
    private static final String INSUFFICIENT_STOCK_MSG = "Stock insuficiente para: %s. Stock disponible: %d";
    private static final String INVALID_QUANTITY_MSG = "Cantidad inválida para: %s";
    private static final String PERMISSION_DENIED_UPDATE_MSG = "No tienes permisos para actualizar esta orden";
    private static final String PERMISSION_DENIED_VIEW_MSG = "No tienes permisos para ver esta orden";
    private static final String PERMISSION_DENIED_CANCEL_MSG = "No tienes permisos para cancelar esta orden";
    private static final String ONLY_PENDING_CAN_CANCEL_MSG = "Solo se pueden cancelar órdenes pendientes";
    private static final String PAYMENT_NOT_VERIFIED_MSG = "Pago no verificado por el gateway";
    private static final String PAYMENT_CONFIRMATION_ERROR_PREFIX = "Error confirmando pago: ";

    // Constantes de logs estructurados
    private static final String LOG_CREATE_ORDER = "🛒 Creando orden para usuario: {}";
    private static final String LOG_ORDER_CREATED = "✅ Orden creada con ID: {} - Número: {}";
    private static final String LOG_SELLER_NOTIFIED = "📦 Notificaciones de nueva orden enviadas a vendedores";
    private static final String LOG_CONFIRM_PAYMENT = "💳 Confirmando pago: {}";
    private static final String LOG_PAYMENT_CONFIRMED = "✅ Pago confirmado y orden {} actualizada a CONFIRMED";
    private static final String LOG_CONFIRMATION_EMAIL_SENT = "Email de confirmación enviado para orden: {}";
    private static final String LOG_CONFIRMATION_EMAIL_ERROR = "❌ Error enviando email de confirmación para orden {}: {}";
    private static final String LOG_STATUS_UPDATE_EMAIL_SENT = "Email de actualización enviado para orden: {} (de {} a {})";
    private static final String LOG_STATUS_UPDATE_EMAIL_ERROR = "❌ Error enviando email de actualización de estado: {}";
    private static final String LOG_STOCK_UPDATED = "Stock actualizado para perfume {}: nuevo stock = {}";
    private static final String LOG_PAYMENT_RECORD_CREATED = "Registro de pago creado para orden: {}";
    private static final String LOG_ORDER_STATUS_UPDATED = "Orden {} actualizada de {} a {} por {}";
    private static final String LOG_ORDER_CANCELLED = "Orden {} cancelada por usuario {}";
    private static final String LOG_SIMULATE_PAYMENT_ERROR = "Error simulando pago: {}";

    // -------------------------------------------------------------------------
    // Helper Methods (DRY)
    // -------------------------------------------------------------------------
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG));
    }

    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(ORDER_NOT_FOUND_MSG));
    }

    private Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException(ORDER_NOT_FOUND_MSG));
    }

    // -------------------------------------------------------------------------
    // Flujo Transaccional de Órdenes
    // -------------------------------------------------------------------------
    @Transactional
    public OrderResponseDTO createOrder(CheckoutRequestDTO checkoutRequest, String username) {
        log.info(LOG_CREATE_ORDER, username);

        User user = getUserByUsername(username);
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
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info(LOG_ORDER_CREATED, savedOrder.getId(), savedOrder.getOrderNumber());

        createOrderItems(savedOrder, calculation.getItems());

        PaymentResponseDTO paymentResponse = paymentGatewayService.createPayment(
                savedOrder, checkoutRequest.getPaymentMethod());

        createPaymentRecord(savedOrder, paymentResponse, checkoutRequest.getPaymentMethod());

        // Notificar inmediatamente a los vendedores sobre la nueva orden PENDIENTE
        if (notificationService != null) {
            notificationService.notifySellerNewOrder(savedOrder);
            log.info(LOG_SELLER_NOTIFIED);
        }

        return buildOrderResponse(savedOrder, paymentResponse);
    }

    @Transactional
    public void confirmPayment(String paymentId) {
        try {
            log.info(LOG_CONFIRM_PAYMENT, paymentId);

            boolean paymentVerified = paymentGatewayService.verifyPayment(paymentId);
            if (!paymentVerified) {
                throw new RuntimeException(PAYMENT_NOT_VERIFIED_MSG);
            }

            Payment payment = paymentRepository.findByPaymentGatewayId(paymentId)
                    .orElseThrow(() -> new RuntimeException(PAYMENT_NOT_FOUND_MSG));

            Order order = payment.getOrder();

            // Actualizar estado del pago
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // Actualizar estado de la orden
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Enviar correo de confirmación de orden
            try {
                emailService.sendOrderConfirmationEmail(order);
                log.info(LOG_CONFIRMATION_EMAIL_SENT, order.getOrderNumber());
            } catch (Exception e) {
                log.error(LOG_CONFIRMATION_EMAIL_ERROR, order.getOrderNumber(), e.getMessage());
            }

            // Notificaciones multicanal
            if (notificationService != null) {
                notificationService.notifySellerNewOrder(order);
                notificationService.notifyPaymentSuccess(order);
                checkLowStockAfterOrder(order);
            }

            log.info(LOG_PAYMENT_CONFIRMED, order.getOrderNumber());

        } catch (Exception e) {
            log.error("❌ " + PAYMENT_CONFIRMATION_ERROR_PREFIX + "{}", e.getMessage(), e);
            throw new RuntimeException(PAYMENT_CONFIRMATION_ERROR_PREFIX + e.getMessage());
        }
    }

    private void checkLowStockAfterOrder(Order order) {
        if (order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            Perfume perfume = item.getPerfume();
            if (perfume != null && perfume.getStock() < LOW_STOCK_THRESHOLD) {
                notificationService.notifyLowStock(perfume);
            }
        }
    }

    public boolean simulatePayment(String paymentIntentId, boolean success) {
        try {
            return paymentGatewayService.simulatePayment(paymentIntentId, success);
        } catch (Exception e) {
            log.error(LOG_SIMULATE_PAYMENT_ERROR, e.getMessage());
            return false;
        }
    }

    private OrderCalculationResult calculateOrderTotals(List<CartItemDTO> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException(EMPTY_CART_MSG);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItemCalculation> calculatedItems = new ArrayList<>();

        for (CartItemDTO cartItem : cartItems) {
            Perfume perfume = perfumeRepository.findById(cartItem.getPerfumeId())
                    .orElseThrow(() -> new RuntimeException(PERFUME_NOT_FOUND_PREFIX_MSG + cartItem.getPerfumeId()));

            if (perfume.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException(String.format(INSUFFICIENT_STOCK_MSG, perfume.getName(), perfume.getStock()));
            }

            if (cartItem.getQuantity() <= 0) {
                throw new RuntimeException(String.format(INVALID_QUANTITY_MSG, perfume.getName()));
            }

            BigDecimal perfumePrice = BigDecimal.valueOf(perfume.getPrice());
            BigDecimal itemTotal = perfumePrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
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

            order.getItems().add(item);
            orderItemRepository.save(item);

            Perfume perfume = calc.getPerfume();
            perfume.setStock(perfume.getStock() - calc.getQuantity());
            perfumeRepository.save(perfume);

            log.info(LOG_STOCK_UPDATED, perfume.getName(), perfume.getStock());
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
        log.info(LOG_PAYMENT_RECORD_CREATED, order.getOrderNumber());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String username) {
        Order order = getOrderById(orderId);
        User user = getUserByUsername(username);

        boolean isSeller = order.getItems().stream()
                .anyMatch(item -> item.getPerfume() != null &&
                        item.getPerfume().getUser() != null &&
                        item.getPerfume().getUser().getId().equals(user.getId()));

        if (!isSeller && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException(PERMISSION_DENIED_UPDATE_MSG);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        try {
            emailService.sendOrderStatusUpdateEmail(updatedOrder, oldStatus, newStatus);
            log.info(LOG_STATUS_UPDATE_EMAIL_SENT, order.getOrderNumber(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error(LOG_STATUS_UPDATE_EMAIL_ERROR, e.getMessage());
        }

        if (notificationService != null) {
            notificationService.notifyOrderStatusUpdate(updatedOrder, order.getUser().getUsername());
        }

        log.info(LOG_ORDER_STATUS_UPDATED, order.getOrderNumber(), oldStatus, newStatus, username);
        return updatedOrder;
    }

    private String generateOrderNumber() {
        return ORDER_NUMBER_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase() +
                "-" + System.currentTimeMillis() % 10000;
    }

    private OrderResponseDTO buildOrderResponse(Order order, PaymentResponseDTO paymentResponse) {
        OrderResponseDTO response = buildBaseOrderResponse(order);
        if (paymentResponse != null) {
            response.setPaymentUrl(paymentResponse.getGatewayUrl());
            response.setClientSecret(paymentResponse.getClientSecret());
        }
        return response;
    }

    private OrderResponseDTO buildBaseOrderResponse(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus().toString());
        dto.setSubtotal(order.getSubtotal());
        dto.setTax(order.getTax());
        dto.setShipping(order.getShipping());
        dto.setTotal(order.getTotal());
        dto.setCreatedAt(order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (order.getItems() != null) {
            List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                    .map(this::convertToOrderItemResponseDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemDTOs);
        }
        return dto;
    }

    private OrderItemResponseDTO convertToOrderItemResponseDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(item.getId());
        if (item.getPerfume() != null) {
            dto.setPerfumeId(item.getPerfume().getId());
            dto.setPerfumeName(item.getPerfume().getName());
            if (item.getPerfume().getBrand() != null) {
                dto.setBrandName(item.getPerfume().getBrand().getName());
            }
            dto.setImageUrl(item.getPerfume().getImageUrl());
        }
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }

    public Page<OrderResponseDTO> getUserOrders(String username, Pageable pageable) {
        getUserByUsername(username); // Validar existencia
        Page<Order> orders = orderRepository.findByUsername(username, pageable);

        List<OrderResponseDTO> orderDTOs = orders.getContent().stream()
                .map(this::buildBaseOrderResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(orderDTOs, pageable, orders.getTotalElements());
    }

    public OrderResponseDTO getOrderByNumber(String orderNumber, String username) {
        Order order = getOrderByNumber(orderNumber);

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException(PERMISSION_DENIED_VIEW_MSG);
        }

        return buildBaseOrderResponse(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String username) {
        Order order = getOrderById(orderId);

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException(PERMISSION_DENIED_CANCEL_MSG);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException(ONLY_PENDING_CAN_CANCEL_MSG);
        }

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Perfume perfume = item.getPerfume();
                if (perfume != null) {
                    perfume.setStock(perfume.getStock() + item.getQuantity());
                    perfumeRepository.save(perfume);
                }
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        paymentRepository.findByOrder(order).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        });

        log.info(LOG_ORDER_CANCELLED, order.getOrderNumber(), username);
    }

    public Page<Order> getSellerOrders(String username, Pageable pageable, OrderStatus status) {
        User seller = getUserByUsername(username);

        if (status != null) {
            return orderRepository.findBySellerAndStatus(seller.getId(), status, pageable);
        } else {
            return orderRepository.findBySeller(seller.getId(), pageable);
        }
    }

    public Order getSellerOrderDetail(Long orderId, String username) {
        Order order = getOrderById(orderId);
        User seller = getUserByUsername(username);

        boolean isSeller = order.getItems().stream()
                .anyMatch(item -> item.getPerfume() != null &&
                        item.getPerfume().getUser() != null &&
                        item.getPerfume().getUser().getId().equals(seller.getId()));

        if (!isSeller && !seller.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException(PERMISSION_DENIED_VIEW_MSG);
        }

        return order;
    }

    public Map<String, Object> getSellerStats(String username) {
        User seller = getUserByUsername(username);

        long totalOrders = orderRepository.countBySeller(seller.getId());
        long pendingOrders = orderRepository.countBySellerAndStatus(seller.getId(), OrderStatus.CONFIRMED);
        long completedOrders = orderRepository.countBySellerAndStatus(seller.getId(), OrderStatus.DELIVERED);

        Double totalRevenue = orderRepository.getTotalRevenueBySeller(seller.getId());
        if (totalRevenue == null) totalRevenue = 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("totalRevenue", totalRevenue);
        stats.put("lastUpdated", LocalDateTime.now());

        return stats;
    }
}
