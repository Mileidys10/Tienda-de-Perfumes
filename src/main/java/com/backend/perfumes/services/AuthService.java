package com.backend.perfumes.services;

import com.backend.perfumes.dto.RegiterDto;
import com.backend.perfumes.model.Role;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

            if (!user.isEmailVerified()) {
                throw new RuntimeException("Debes verificar tu correo electrónico antes de iniciar sesión. Revisa tu bandeja de entrada.");
            }

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
        System.out.println("Email: " + request.getEmail());
        System.out.println("Nombre: " + request.getName());
        System.out.println("Apellido: " + request.getLastName());

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
        newUser.setEmailVerified(false);

        String verificationToken = UUID.randomUUID().toString();
        newUser.setVerificationToken(verificationToken);
        newUser.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(newUser);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
            System.out.println("Email de verificación enviado a: " + savedUser.getEmail());
        } catch (Exception e) {
            System.err.println("Error enviando email de verificación: " + e.getMessage());
        }

        System.out.println("=== REGISTRO EXITOSO ===");
        System.out.println("Usuario ID: " + savedUser.getId());
        System.out.println("Email: " + savedUser.getEmail());
        System.out.println("Rol: " + savedUser.getRole());
        return savedUser;
    }

    public String verifyAccount(String token) {
        System.out.println("=== VERIFICANDO CUENTA ===");
        System.out.println("Token recibido: " + token);

        try {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Token de verificación inválido o expirado"));

            if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("El token de verificación ha expirado");
            }

            user.setEmailVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpiry(null);
            userRepository.save(user);

            System.out.println("=== CUENTA VERIFICADA EXITOSAMENTE ===");
            System.out.println("Usuario: " + user.getEmail());
            return "¡Cuenta verificada con éxito! Ya puedes iniciar sesión.";

        } catch (Exception e) {
            System.out.println("=== ERROR EN VERIFICACIÓN: " + e.getMessage() + " ===");
            throw new RuntimeException("Error al verificar la cuenta: " + e.getMessage());
        }
    }

    public String resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("El email ya está verificado");
        }

        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), newToken);

        return "Email de verificación reenviado. Revisa tu bandeja de entrada.";
    }

    public String requestAccountDeletion(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String deleteToken = UUID.randomUUID().toString();

        user.setVerificationToken(deleteToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendDeletionEmail(user.getEmail(), deleteToken);

        return "Se ha enviado un enlace de confirmación para eliminar tu cuenta a tu correo electrónico.";
    }

    public String deleteAccount(String token) {
        try {
            // Buscar usuario por token de eliminación
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Token de eliminación inválido o expirado"));

            userRepository.delete(user);

            System.out.println("=== CUENTA ELIMINADA ===");
            System.out.println("Usuario eliminado: " + user.getEmail());
            return "Cuenta eliminada exitosamente";

        } catch (Exception e) {
            System.out.println("=== ERROR ELIMINANDO CUENTA: " + e.getMessage() + " ===");
            throw new RuntimeException("Error al eliminar la cuenta: " + e.getMessage());
        }
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}