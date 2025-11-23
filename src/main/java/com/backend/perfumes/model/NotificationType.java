package com.backend.perfumes.model;


public enum NotificationType {
    NEW_ORDER,      // Nueva orden para vendedor
    ORDER_UPDATE,   // Actualización de estado para cliente
    PAYMENT_SUCCESS,// Pago exitoso
    STOCK_ALERT     // Alerta de stock bajo
}