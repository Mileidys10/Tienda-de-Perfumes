package com.backend.perfumes.services;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderEmailTemplateServiceTest {

    @Autowired
    private OrderEmailTemplateService orderEmailTemplateService;

    @Test
    @DisplayName("Renderizado de plantilla Thymeleaf de confirmacion de orden")
    void testBuildOrderConfirmationTemplate() {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST-12345");
        order.setCustomerEmail("cliente@correo.com");
        order.setCustomerPhone("+57 300 123 4567");
        order.setShippingAddress("Calle 100 # 15-20, Bogota");
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        order.setSubtotal(BigDecimal.valueOf(100.00));
        order.setTax(BigDecimal.valueOf(16.00));
        order.setShipping(BigDecimal.valueOf(5.00));
        order.setTotal(BigDecimal.valueOf(121.00));
        order.setItems(new ArrayList<>());

        String html = orderEmailTemplateService.buildOrderConfirmationTemplate(order);

        assertNotNull(html, "El HTML generado no debe ser nulo");
        assertTrue(html.contains("ORD-TEST-12345"), "Debe contener el numero de orden");
        assertTrue(html.contains("121.00") || html.contains("121"), "Debe contener el total");
        assertTrue(html.contains("Calle 100 # 15-20"), "Debe contener la direccion de envio");
    }

    @Test
    @DisplayName("Renderizado de plantilla Thymeleaf de actualizacion de estado")
    void testBuildOrderStatusUpdateTemplate() {
        Order order = new Order();
        order.setOrderNumber("ORD-UPDATE-999");
        order.setCustomerEmail("cliente@correo.com");
        order.setShippingAddress("Carrera 50 # 80-10, Medellin");
        order.setStatus(OrderStatus.SHIPPED);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotal(BigDecimal.valueOf(250.00));
        order.setItems(new ArrayList<>());

        String html = orderEmailTemplateService.buildOrderStatusUpdateTemplate(
                order,
                OrderStatus.PREPARING,
                OrderStatus.SHIPPED
        );

        assertNotNull(html, "El HTML de actualizacion no debe ser nulo");
        assertTrue(html.contains("ORD-UPDATE-999"), "Debe contener el numero de orden");
        assertTrue(html.contains("Enviada"), "Debe contener el nuevo estado en espanol");
        assertTrue(html.contains(OrderStatus.SHIPPED.getColor()), "Debe contener el color del estado");
    }
}
