package com.backend.perfumes.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    @DisplayName("Cada estado de orden debe tener color, icono, nombre legible y mensaje no nulos")
    void testOrderStatusMetadata() {
        for (OrderStatus status : OrderStatus.values()) {
            assertNotNull(status.getColor(), "Color no debe ser nulo para " + status);
            assertTrue(status.getColor().startsWith("#"), "Color debe ser formato hexadecimal para " + status);
            assertNotNull(status.getIcon(), "Icono no debe ser nulo para " + status);
            assertNotNull(status.getDisplayName(), "DisplayName no debe ser nulo para " + status);
            assertNotNull(status.getMessage(), "Mensaje no debe ser nulo para " + status);
        }
    }

    @Test
    @DisplayName("Valores especificos esperados para estados clave")
    void testSpecificStatusValues() {
        assertEquals("Pendiente", OrderStatus.PENDING.getDisplayName());
        assertEquals("Confirmada", OrderStatus.CONFIRMED.getDisplayName());
        assertEquals("Enviada", OrderStatus.SHIPPED.getDisplayName());
        assertEquals("Entregada", OrderStatus.DELIVERED.getDisplayName());
        assertEquals("Cancelada", OrderStatus.CANCELLED.getDisplayName());
    }
}
