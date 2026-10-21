package com.backend.perfumes.utils;

import com.backend.perfumes.model.User;

import java.util.HashMap;
import java.util.Map;

public class AuthReponseBuilder {

    public static Map<String, Object> buildAuthResponse(String accessToken, String refreshToken, User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600); // 1 hora en segundos
        response.put("usuario", buildUsuarioResponse(user));
        return response;
    }

    // Método sobrecargado para mantener compatibilidad con código existente
    public static Map<String, Object> buildAuthResponse(String token, User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("usuario", buildUsuarioResponse(user));
        return response;
    }

    private static Map<String, Object> buildUsuarioResponse(User usuario) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", usuario.getId());
        userMap.put("email", usuario.getEmail());
        userMap.put("rol", usuario.getRole().name());
        userMap.put("name", usuario.getName());
        userMap.put("apellido", usuario.getLastName());
        userMap.put("username", usuario.getUsername());
        userMap.put("emailVerified", usuario.isEmailVerified());
        userMap.put("active", usuario.isActive());
        return userMap;
    }

    // Método adicional para respuestas de refresh token
    public static Map<String, Object> buildRefreshTokenResponse(String accessToken, String refreshToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Tokens refrescados exitosamente");
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600);
        return response;
    }
}