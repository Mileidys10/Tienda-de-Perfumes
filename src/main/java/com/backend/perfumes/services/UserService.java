package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final JwtService  jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final int otpExpiryMinutes = 10;


    public UserService(UserRepository userRepository,
                       JwtService jwtService,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;

    }

    private String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }



    public String requestDeletion( String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateDeleteAccountToken(user);
        emailService.sendDeletionEmail(email, token);
        return "Solicitud de eliminar Usuario enviada";
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





    public String verifyToken(String token) {
        if (!jwtService.isDeleteToken(token)) {
            throw new RuntimeException("Token invalido");

        }

        Claims claims = jwtService.extractAllClaims(token);
        Long userId = claims.get("id", Long.class);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


        userRepository.delete(user);


        return "Usuario eliminado con exito";



    }


}
