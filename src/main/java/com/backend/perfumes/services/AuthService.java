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
import java.util.HashMap;
import java.util.Map;
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

            /*
            if (!user.isEmailVerified()) {
                throw new RuntimeException("Debes verificar tu correo electrónico antes de iniciar sesión. Revisa tu bandeja de entrada.");
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
        // newUser.setEmailVerified(false);
        newUser.setEmailVerified(true); // Marcamos como verificado automáticamente

        /*
        String code = generateOtp();
        String hashedOtp = passwordEncoder.encode(code);

        newUser.setVerificationCode(hashedOtp);
        newUser.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        */

        User savedUser = userRepository.save(newUser);

        /*
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), code);
            System.out.println("Email de verificación enviado a: " + savedUser.getEmail());
        } catch (Exception e) {
            System.err.println("Error enviando email de verificación: " + e.getMessage());
        }
        */

        System.out.println("=== REGISTRO EXITOSA ===");
        System.out.println("Usuario ID: " + savedUser.getId());
        System.out.println("Email: " + savedUser.getEmail());
        System.out.println("Rol: " + savedUser.getRole());
        return savedUser;
    }

    public String verifyAccount(String email, String code) {
        /*
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getVerificationCode() == null) {
            throw new RuntimeException("No hay verificación pendiente.");
        }

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
        */

        return "¡Cuenta verificada con éxito! (Verificación temporalmente deshabilitada)";
    }

    public String resendVerificationEmail(String email) {
        /*
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
        */

        return "Código de verificación reenviado. (Funcionalidad temporalmente deshabilitada)";
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public Map<String, String> refreshToken(String refreshToken) {
        try {
            System.out.println("=== INICIANDO REFRESCO DE TOKEN ===");

            // Validar que el token sea un refresh token
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Token no válido para refresh");
            }

            // Extraer username del refresh token
            String username = jwtService.extractUsername(refreshToken);
            System.out.println("Username extraído: " + username);

            // Buscar usuario
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar refresh token
            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new RuntimeException("Refresh token inválido o expirado");
            }

            if (!user.isActive()) {
                throw new RuntimeException("Tu cuenta está desactivada. Contacta al administrador.");
            }

            // Generar nuevos tokens
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", user.getRole());
            extraClaims.put("nombre", user.getName());
            extraClaims.put("email", user.getEmail());

            String newAccessToken = jwtService.generateToken(extraClaims, user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            System.out.println("=== REFRESCO DE TOKEN EXITOSO ===");

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);

            return tokens;

        } catch (RuntimeException e) {
            System.out.println("=== ERROR EN REFRESCO: " + e.getMessage() + " ===");
            throw e;
        } catch (Exception e) {
            System.out.println("=== ERROR EN REFRESCO: " + e.getMessage() + " ===");
            e.printStackTrace();
            throw new RuntimeException("Error en el servidor durante el refresco de token");
        }
    }

}