package com.backend.perfumes.services;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
public class OrderEmailTemplateService {

    private final TemplateEngine templateEngine;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public OrderEmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String buildOrderConfirmationTemplate(Order order) {
        Context context = new Context();

        context.setVariable("order", order);
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("status", order.getStatus() != null ? order.getStatus().getDisplayName() : "Pendiente");
        context.setVariable("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : "");
        context.setVariable("items", order.getItems() != null ? order.getItems() : Collections.emptyList());
        context.setVariable("subtotal", order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO);
        context.setVariable("tax", order.getTax() != null ? order.getTax() : BigDecimal.ZERO);
        context.setVariable("shipping", order.getShipping() != null ? order.getShipping() : BigDecimal.ZERO);
        context.setVariable("total", order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO);
        context.setVariable("shippingAddress", order.getShippingAddress() != null ? order.getShippingAddress() : "");

        return templateEngine.process("mail/order-confirmation", context);
    }

    public String buildOrderStatusUpdateTemplate(
            Order order,
            OrderStatus oldStatus,
            OrderStatus newStatus
    ) {
        Context context = new Context();

        context.setVariable("order", order);
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("oldStatus", oldStatus != null ? oldStatus.getDisplayName() : "");
        context.setVariable("newStatus", newStatus != null ? newStatus.getDisplayName() : "");
        context.setVariable("statusColor", newStatus != null ? newStatus.getColor() : "#667eea");
        context.setVariable("statusIcon", newStatus != null ? newStatus.getIcon() : "📦");
        context.setVariable("statusMessage", newStatus != null ? newStatus.getMessage() : "Tu orden ha sido actualizada.");
        context.setVariable("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().format(FORMATTER) : "");
        context.setVariable("total", order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO);
        context.setVariable("shippingAddress", order.getShippingAddress() != null ? order.getShippingAddress() : "");
        context.setVariable("totalProducts", order.getItems() != null ? order.getItems().size() : 0);

        return templateEngine.process("mail/order-status-update", context);
    }
}
