package com.backend.perfumes.services;

import com.backend.perfumes.dto.RegiterDto;
import com.backend.perfumes.model.Role;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;

    private static final int OTP_EXPIRATION_MINUTES = 10;
    private static final int OTP_MIN_VALUE = 100000;
    private static final int OTP_RANGE = 900000;

    private static final String INACTIVE_ACCOUNT_MSG = "Tu cuenta esta desactivada. Contacta al administrador.";
    private static final String INVALID_CREDENTIALS_MSG = "CREDENCIALES INVALIDAS";
    private static final String SERVER_ERROR_MSG = "Error en el servidor durante la autenticacion";
    private static final String EMAIL_IN_USE_MSG = "El email ya esta registrado";
    private static final String EMAIL_VERIFICATION_REQUIRED_MSG = "Debes verificar tu correo electronico antes de iniciar sesion. Revisa tu bandeja de entrada.";
    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado";
    private static final String NO_PENDING_VERIFICATION_MSG = "No hay verificacion pendiente.";
    private static final String VERIFICATION_CODE_EXPIRED_MSG = "El codigo ha expirado";
    private static final String INVALID_VERIFICATION_CODE_MSG = "Codigo invalido";
    private static final String ACCOUNT_VERIFIED_SUCCESSFULLY_MSG = "Cuenta verificada con exito!";
    private static final String EMAIL_ALREADY_VERIFIED_MSG = "El email ya esta verificado";
    private static final String VERIFICATION_CODE_RESENT_MSG = "Codigo de verificacion reenviado";
    private static final String INVALID_REFRESH_TOKEN_MSG = "Token no valido para refresh";
    private static final String REFRESH_TOKEN_INVALID_OR_EXPIRED_MSG = "Refresh token invalido o expirado";
    private static final String TOKEN_REFRESH_SERVER_ERROR_MSG = "Error en el servidor durante el refresco de token";

    private String generateOtp() {
        return String.valueOf((int)(Math.random() * OTP_RANGE) + OTP_MIN_VALUE);
    }

    public User authenticate(String email, String password) {
        try {
            log.info("=== INICIANDO AUTENTICACION === Email recibido: {}", email);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            User user = (User) authentication.getPrincipal();
            log.info("Usuario autenticado: {} | Email verificado: {} | Cuenta activa: {}", 
                    user.getEmail(), user.isEmailVerified(), user.isActive());

            if (!user.isEmailVerified()) {
                throw new RuntimeException(EMAIL_VERIFICATION_REQUIRED_MSG);
            }

            if (!user.isActive()) {
                throw new RuntimeException(INACTIVE_ACCOUNT_MSG);
            }

            log.info("=== AUTENTICACION EXITOSA === para usuario: {}", user.getEmail());
            return user;

        } catch (BadCredentialsException e) {
            throw new RuntimeException(INVALID_CREDENTIALS_MSG);
        } catch (RuntimeException e) {
            log.warn("Error en autenticacion: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("=== ERROR EN AUTENTICACION: {}", e.getMessage(), e);
            throw new RuntimeException(SERVER_ERROR_MSG);
        }
    }

    public User register(RegiterDto request) {
        log.info("=== INICIANDO REGISTRO === Email: {} | Nombre: {} | Apellido: {}", 
                request.getEmail(), request.getName(), request.getLastName());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException(EMAIL_IN_USE_MSG);
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
        newUser.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));

        User savedUser = userRepository.save(newUser);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), code);
            log.info("Email de verificacion enviado a: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Error enviando email de verificacion a {}: {}", savedUser.getEmail(), e.getMessage());
        }

        log.info("=== REGISTRO EXITOSO === ID: {} | Email: {} | Rol: {}", 
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        return savedUser;
    }

    public String verifyAccount(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG));

        if (user.getVerificationCode() == null) {
            throw new RuntimeException(NO_PENDING_VERIFICATION_MSG);
        }

        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException(VERIFICATION_CODE_EXPIRED_MSG);
        }

        if (!passwordEncoder.matches(code, user.getVerificationCode())) {
            throw new RuntimeException(INVALID_VERIFICATION_CODE_MSG);
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);

        userRepository.save(user);
        log.info("Cuenta verificada con exito para email: {}", email);

        return ACCOUNT_VERIFIED_SUCCESSFULLY_MSG;
    }

    public String resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG));

        if (user.isEmailVerified()) {
            throw new RuntimeException(EMAIL_ALREADY_VERIFIED_MSG);
        }

        String code = generateOtp();
        String hashedOtp = passwordEncoder.encode(code);

        user.setVerificationCode(hashedOtp);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), code);
        log.info("Codigo de verificacion reenviado a: {}", email);

        return VERIFICATION_CODE_RESENT_MSG;
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public Map<String, String> refreshToken(String refreshToken) {
        try {
            log.info("=== INICIANDO REFRESCO DE TOKEN ===");

            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new RuntimeException(INVALID_REFRESH_TOKEN_MSG);
            }

            String username = jwtService.extractUsername(refreshToken);
            log.info("Username extraido del refresh token: {}", username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MSG));

            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new RuntimeException(REFRESH_TOKEN_INVALID_OR_EXPIRED_MSG);
            }

            if (!user.isActive()) {
                throw new RuntimeException(INACTIVE_ACCOUNT_MSG);
            }

            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", user.getRole());
            extraClaims.put("nombre", user.getName());
            extraClaims.put("email", user.getEmail());

            String newAccessToken = jwtService.generateToken(extraClaims, user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            log.info("=== REFRESCO DE TOKEN EXITOSO === para usuario: {}", username);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);

            return tokens;

        } catch (RuntimeException e) {
            log.warn("Error durante el refresco de token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("=== ERROR EN REFRESCO DE TOKEN: {}", e.getMessage(), e);
            throw new RuntimeException(TOKEN_REFRESH_SERVER_ERROR_MSG);
        }
    }
}
