package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final int otpExpiryMinutes = 10;


    public UserService(UserRepository userRepository,
                       JwtService jwtService,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;

    }

    private String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }



    public String requestDeletion( String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String code = generateOtp();
        String hashed = passwordEncoder.encode(code);

        user.setDeletionCode(hashed);
        user.setDeletionCodeExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        userRepository.save(user);

        emailService.sendDeletionEmail(email, code);

        return "Código de verificación enviado al correo.";
    }


    public String requestChangeEmail(String username, String newEmail) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("El email ya está en uso");
        }

        String code = generateOtp();
        String hashed = passwordEncoder.encode(code);

        user.setPendingEmail(newEmail);
        user.setEmailUpdateCode(hashed);
        user.setEmailUpdateCodeExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        userRepository.save(user);

        emailService.sendUpdateCode(newEmail, code);

        return "Código enviado al nuevo correo.";
    }


    public String confirmChangeEmail(String code) {

        User user = userRepository.findByPendingEmailIsNotNull()
                .orElseThrow(() -> new RuntimeException("No hay solicitudes pendientes"));

        if (user.getEmailUpdateCodeExpiry().isBefore(LocalDateTime.now())) {
            clearEmailUpdateFields(user);
            throw new RuntimeException("Código expirado");
        }

        if (!passwordEncoder.matches(code, user.getEmailUpdateCode())) {
            throw new RuntimeException("Código inválido");
        }

        user.setEmail(user.getPendingEmail());
        clearEmailUpdateFields(user);

        userRepository.save(user);
        return "Correo actualizado correctamente.";
    }


    private void clearEmailUpdateFields(User user) {
        user.setPendingEmail(null);
        user.setEmailUpdateCode(null);
        user.setEmailUpdateCodeExpiry(null);
    }





    public String ConfirmDeleteUser(String code) {

        User user = userRepository.findByDeletionCodeIsNotNull()
                .orElseThrow(() -> new RuntimeException("No hay solicitudes de eliminación pendientes"));

        if (user.getDeletionCodeExpiry().isBefore(LocalDateTime.now())) {
            clearDeletionFields(user);
            userRepository.save(user);
            throw new RuntimeException("Código expirado");
        }

        if (!passwordEncoder.matches(code, user.getDeletionCode())) {
            throw new RuntimeException("Código incorrecto");
        }

        userRepository.delete(user);

        return "Usuario eliminado con éxito.";
    }

    private void clearDeletionFields(User user) {
        user.setDeletionCode(null);
        user.setDeletionCodeExpiry(null);
    }






}
