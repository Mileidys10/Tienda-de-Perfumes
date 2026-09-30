package com.backend.perfumes.model;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING(
            "#ffc107",
            "⏳",
            "Pendiente",
            "Tu orden está pendiente de confirmación."
    ),

    CONFIRMED(
            "#28a745",
            "✅",
            "Confirmada",
            "Tu orden ha sido confirmada."
    ),

    PREPARING(
            "#17a2b8",
            "📦",
            "Preparando",
            "Tu orden está siendo preparada."
    ),

    SHIPPED(
            "#007bff",
            "🚚",
            "Enviada",
            "Tu orden está en camino."
    ),

    DELIVERED(
            "#28a745",
            "✅",
            "Entregada",
            "Tu orden ha sido entregada."
    ),

    CANCELLED(
            "#dc3545",
            "❌",
            "Cancelada",
            "Tu orden fue cancelada."
    ),

    REFUNDED(
            "#6c757d",
            "💰",
            "Reembolsada",
            "Tu orden fue reembolsada."
    );

    private final String color;
    private final String icon;
    private final String displayName;
    private final String message;

    OrderStatus(String color, String icon, String displayName, String message) {
        this.color = color;
        this.icon = icon;
        this.displayName = displayName;
        this.message = message;
    }

}