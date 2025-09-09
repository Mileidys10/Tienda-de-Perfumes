package com.backend.perfumes.utils;

import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.model.User;

import java.util.HashMap;
import java.util.Map;
public class AuthReponseBuilder {

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
        return userMap;
    }
}
