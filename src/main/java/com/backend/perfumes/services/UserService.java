package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Constantes para OTP
    private static final int OTP_EXPIRATION_MINUTES = 10;
    private static final int OTP_MIN_VALUE = 100000;
    private static final int OTP_RANGE = 900000;

    // Constantes de mensajes
    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado";
    private static final String EMAIL_IN_USE_MSG = "El email ya está en uso";
    private static final String NO_PENDING_REQUEST_MSG = "No hay solicitudes pendientes";
    private static final String NO_PENDING_DELETION_MSG = "No hay solicitudes de eliminación pendientes";
    private static final String CODE_EXPIRED_MSG = "Código expirado";
    private static final String INVALID_CODE_MSG = "Código inválido";
    private static final String INCORRECT_CODE_MSG = "Código incorrecto";

    private static final String DELETION_CODE_SENT_MSG = "Código de verificación enviado al correo.";
    private static final String EMAIL_UPDATE_CODE_SENT_MSG = "Código enviado al nuevo correo.";
    private static final String EMAIL_UPDATED_SUCCESS_MSG = "Correo actualizado correctamente.";
    private static final String USER_DELETED_SUCCESS_MSG = "Usuario eliminado con éxito.";

    // -------------------------------------------------------------------------
    // Helper Methods (DRY)
    // -------------------------------------------------------------------------
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG));
    }

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * OTP_RANGE) + OTP_MIN_VALUE);
    }

    private void clearEmailUpdateFields(User user) {
        user.setPendingEmail(null);
        user.setEmailUpdateCode(null);
        user.setEmailUpdateCodeExpiry(null);
    }

    private void clearDeletionFields(User user) {
        user.setDeletionCode(null);
        user.setDeletionCodeExpiry(null);
    }

    // -------------------------------------------------------------------------
    // Operaciones de Usuario
    // -------------------------------------------------------------------------
    public String requestDeletion(String email) {
        User user = getUserByEmail(email);

        String code = generateOtp();
        String hashed = passwordEncoder.encode(code);

        user.setDeletionCode(hashed);
        user.setDeletionCodeExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));
        userRepository.save(user);

        emailService.sendDeletionEmail(email, code);
        log.info("Código de eliminación solicitado para: {}", email);

        return DELETION_CODE_SENT_MSG;
    }

    public String requestChangeEmail(String username, String newEmail) {
        User user = getUserByUsername(username);

        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException(EMAIL_IN_USE_MSG);
        }

        String code = generateOtp();
        String hashed = passwordEncoder.encode(code);

        user.setPendingEmail(newEmail);
        user.setEmailUpdateCode(hashed);
        user.setEmailUpdateCodeExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));
        userRepository.save(user);

        emailService.sendUpdateCode(newEmail, code);
        log.info("Código de cambio de email solicitado por '{}' para nuevo email: {}", username, newEmail);

        return EMAIL_UPDATE_CODE_SENT_MSG;
    }

    public String confirmChangeEmail(String username, String code) {
        User user = getUserByUsername(username);

        if (user.getPendingEmail() == null) {
            throw new RuntimeException(NO_PENDING_REQUEST_MSG);
        }

        if (user.getEmailUpdateCodeExpiry().isBefore(LocalDateTime.now())) {
            clearEmailUpdateFields(user);
            userRepository.save(user);
            throw new RuntimeException(CODE_EXPIRED_MSG);
        }

        if (!passwordEncoder.matches(code, user.getEmailUpdateCode())) {
            throw new RuntimeException(INVALID_CODE_MSG);
        }

        String oldEmail = user.getEmail();
        user.setEmail(user.getPendingEmail());
        clearEmailUpdateFields(user);
        userRepository.save(user);

        log.info("Email de usuario '{}' actualizado de '{}' a '{}'", username, oldEmail, user.getEmail());
        return EMAIL_UPDATED_SUCCESS_MSG;
    }

    public String confirmDeleteUser(String email, String code) {
        User user = getUserByEmail(email);

        if (user.getDeletionCode() == null) {
            throw new RuntimeException(NO_PENDING_DELETION_MSG);
        }

        if (user.getDeletionCodeExpiry().isBefore(LocalDateTime.now())) {
            clearDeletionFields(user);
            userRepository.save(user);
            throw new RuntimeException(CODE_EXPIRED_MSG);
        }

        if (!passwordEncoder.matches(code, user.getDeletionCode())) {
            throw new RuntimeException(INCORRECT_CODE_MSG);
        }

        userRepository.delete(user);
        log.info("Usuario con email '{}' eliminado permanentemente.", email);
        return USER_DELETED_SUCCESS_MSG;
    }
}
