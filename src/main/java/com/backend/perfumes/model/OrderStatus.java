// OrderStatus.java - Actualizar enum con más estados
package com.backend.perfumes.model;

public enum OrderStatus {
    PENDING,           // Pendiente de pago
    CONFIRMED,         // Pago confirmado, esperando preparación
    PREPARING,         // En preparación por el vendedor
    SHIPPED,           // Enviado/En camino
    DELIVERED,         // Entregado
    CANCELLED,         // Cancelado
    REFUNDED           // Reembolsado
}