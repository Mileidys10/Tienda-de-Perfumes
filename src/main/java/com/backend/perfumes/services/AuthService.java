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



    private String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
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

        String code = generateOtp();
        String hashedOtp = passwordEncoder.encode(code);

        newUser.setVerificationCode(hashedOtp);
        newUser.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));

        User savedUser = userRepository.save(newUser);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), code);
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

    public String verifyAccount(String code) {
        System.out.println("=== VERIFICANDO CUENTA ===");
        System.out.println("Codigo recibido: " + code);

        try {
            User user = userRepository.findByVerificationCodeIsNotNull()
                    .orElseThrow(() -> new RuntimeException("No hay verificación pendiente."));

            if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("El código ha expirado");
            }

            if (!passwordEncoder.matches(code, user.getVerificationCode())) {
                throw new RuntimeException("Código inválido");
            }

            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiry(null);

            userRepository.save(user);

            System.out.println("=== CUENTA VERIFICADA EXITOSAMENTE ===");
            return "¡Cuenta verificada con éxito! Ya puedes iniciar sesión.";

        } catch (Exception e) {
            throw new RuntimeException("Error al verificar la cuenta: " + e.getMessage());
        }
    }

    public String resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("El email ya está verificado");
        }

        String code = generateOtp();
        String hashedOtp = passwordEncoder.encode(code);

        user.setVerificationCode(hashedOtp);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), code);

        return "Código de verificación reenviado.";
    }



    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}