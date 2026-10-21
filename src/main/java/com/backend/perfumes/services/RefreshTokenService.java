package com.backend.perfumes.services;

import com.backend.perfumes.model.RefreshToken;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.RefreshTokenRepository;
import com.backend.perfumes.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado con ID: ";
    private static final String TOKEN_EXPIRED_MSG = "El token de actualización ha expirado. Por favor inicia sesión nuevamente.";

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG + userId));
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(Long userId) {
        User user = getUserById(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token generado exitosamente para usuario ID: {}", userId);
        return saved;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expirado y eliminado para usuario: {}", token.getUser().getUsername());
            throw new RuntimeException(TOKEN_EXPIRED_MSG);
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        User user = getUserById(userId);
        return refreshTokenRepository.deleteByUsuario(user);
    }
}
