package com.backend.perfumes.services;

import com.backend.perfumes.dto.RegiterDto;
import com.backend.perfumes.model.Role;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public User authenticate(String email, String password) {
        try {
            System.out.println("=== INICIANDO AUTENTICACIÓN ===");
            System.out.println("Email recibido: " + email);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            User user = (User) authentication.getPrincipal();
            System.out.println("Usuario autenticado: " + user.getEmail());
            System.out.println("Email verificado: " + user.isEmailVerified());
            System.out.println("Cuenta activa: " + user.isActive());

            /*
            if (!user.isEmailVerified()) {
                throw new RuntimeException("Debes verificar tu correo electrónico antes de iniciar sesión.");
            }
            */

            if (!user.isActive()) {
                throw new RuntimeException("Tu cuenta está desactivada. Contacta al administrador.");
            }

            System.out.println("=== AUTENTICACIÓN EXITOSA ===");
            return user;

        } catch (BadCredentialsException e) {
            System.out.println("=== CREDENCIALES INVÁLIDAS ===");
            throw new RuntimeException("Credenciales inválidas");
        } catch (RuntimeException e) {
            System.out.println("=== ERROR PERSONALIZADO: " + e.getMessage() + " ===");
            throw e;
        } catch (Exception e) {
            System.out.println("=== ERROR EN AUTENTICACIÓN: " + e.getMessage() + " ===");
            e.printStackTrace();
            throw new RuntimeException("Error en el servidor durante la autenticación");
        }
    }

    public User register(RegiterDto request) {
        System.out.println("=== INICIANDO REGISTRO ===");

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());

        String autoUsername = request.getEmail().split("@")[0];
        newUser.setUsername(autoUsername);

        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                newUser.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                newUser.setRole(Role.CLIENTE);
            }
        } else {
            newUser.setRole(Role.CLIENTE);
        }

        newUser.setActive(true);

        newUser.setEmailVerified(true);
        newUser.setVerificationToken(null);
        newUser.setVerificationTokenExpiry(null);

        /*
        String verificationToken = UUID.randomUUID().toString();
        newUser.setVerificationToken(verificationToken);
        newUser.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        */

        User savedUser = userRepository.save(newUser);

        /*
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        } catch (Exception e) {
            System.err.println("Error enviando email de verificación: " + e.getMessage());
        }
        */

        System.out.println("=== REGISTRO EXITOSO ===");
        return savedUser;
    }


    public String verifyAccount(String token) {
        return "La verificación de cuenta está desactivada.";
    }

    public String resendVerificationEmail(String email) {
        return "La verificación de correo está desactivada, no se envía ningún email.";
    }

    public String requestAccountDeletion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String deleteToken = UUID.randomUUID().toString();

        user.setVerificationToken(deleteToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendDeletionEmail(user.getEmail(), deleteToken);

        return "Se envió un enlace para eliminar la cuenta.";
    }

    public String deleteAccount(String token) {
        try {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

            userRepository.delete(user);
            return "Cuenta eliminada exitosamente";

        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar la cuenta: " + e.getMessage());
        }
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
