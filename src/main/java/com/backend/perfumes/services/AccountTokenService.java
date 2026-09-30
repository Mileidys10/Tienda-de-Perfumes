package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountTokenService {

    private static final String DELETE_PREFIX = "del_";

    public String generateVerificationToken(User user) {
        return UUID.randomUUID().toString();
    }

    public String generateDeleteAccountToken(User user) {
        return DELETE_PREFIX + UUID.randomUUID().toString();
    }

    public boolean isDeleteToken(String token) {
        return token != null && token.startsWith(DELETE_PREFIX);
    }

    public boolean isVerificationToken(String token) {
        return token != null && !token.startsWith(DELETE_PREFIX);
    }

    public boolean isValidUUID(String token) {
        if (token == null) {
            return false;
        }
        try {
            if (token.startsWith(DELETE_PREFIX)) {
                UUID.fromString(token.substring(DELETE_PREFIX.length()));
            } else {
                UUID.fromString(token);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
