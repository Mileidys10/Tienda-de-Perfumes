package com.backend.perfumes.controller;

import com.backend.perfumes.dto.LoginRequest;
import com.backend.perfumes.dto.RegiterDto;
import com.backend.perfumes.model.User;
import com.backend.perfumes.services.AuthService;
import com.backend.perfumes.services.EmailService;
import com.backend.perfumes.services.JwtService;
import com.backend.perfumes.utils.AuthReponseBuilder;
import com.backend.perfumes.utils.ErrorReponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "1. Autenticación", description = "Endpoints para login, registro y gestión de tokens de usuarios")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final EmailService emailService;

    // Constantes de mensajes
    private static final String INTERNAL_SERVER_ERROR_MSG = "Error interno del servidor";
    private static final String SUCCESSFUL_REGISTER_MSG = "Usuario registrado correctamente. Por favor verifica tu email antes de iniciar sesión.";
    private static final String REFRESH_TOKEN_MISSING_MSG = "Refresh token es requerido";

    @Operation(
            summary = "Login de usuarios (ADMIN, VENDEDOR, CLIENTE)",
            description = "Autentica un usuario y devuelve sus tokens JWT (access y refresh) junto a la información del perfil",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login exitoso",
                            content = @Content(schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta no activa",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Intento de login para email: {}", request.getEmail());

            User usuario = authService.authenticate(request.getEmail(), request.getPassword());

            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", usuario.getRole());
            extraClaims.put("nombre", usuario.getName());
            extraClaims.put("email", usuario.getEmail());

            String accessToken = jwtService.generateToken(extraClaims, usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);

            log.info("Login exitoso para usuario: {}, Rol: {}", usuario.getUsername(), usuario.getRole());
            return ResponseEntity.ok(AuthReponseBuilder.buildAuthResponse(accessToken, refreshToken, usuario));

        } catch (RuntimeException e) {
            log.warn("Fallo de autenticación para email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED));
        } catch (Exception e) {
            log.error("Error crítico durante login para {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorReponseBuilder.buildErrorResponse(INTERNAL_SERVER_ERROR_MSG, HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "Registro de usuarios", description = "Crea un nuevo usuario (ADMIN, VENDEDOR o CLIENTE)")
    @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegiterDto request) {
        try {
            log.info("Iniciando registro para email: {}", request.getEmail());
            User newUser = authService.register(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", SUCCESSFUL_REGISTER_MSG);
            response.put("email", newUser.getEmail());
            response.put("userId", newUser.getId());

            log.info("Usuario registrado exitosamente con ID: {}", newUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.warn("Error en registro para email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Error crítico durante registro: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorReponseBuilder.buildErrorResponse(INTERNAL_SERVER_ERROR_MSG, HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Operation(summary = "Verificación de cuenta", description = "Verifica la cuenta del usuario mediante un código OTP enviado al correo")
    @PostMapping("/verify-account")
    public ResponseEntity<?> verifyAccount(@RequestParam String email, @RequestParam String code) {
        try {
            log.info("Verificando cuenta para email: {}", email);
            String result = authService.verifyAccount(email, code);

            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Fallo de verificación para {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @Operation(summary = "Reenviar código de verificación", description = "Genera y reenvía un nuevo código OTP al correo del usuario")
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        try {
            log.info("Reenviando código de verificación a: {}", email);
            String result = authService.resendVerificationEmail(email);

            Map<String, String> response = new HashMap<>();
            response.put("message", result);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Fallo al reenviar código para {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @Operation(summary = "Refresco de tokens JWT", description = "Emite un nuevo par de tokens a partir de un refresh token válido")
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorReponseBuilder.buildErrorResponse(REFRESH_TOKEN_MISSING_MSG, HttpStatus.BAD_REQUEST));
            }

            Map<String, String> tokens = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(tokens);

        } catch (RuntimeException e) {
            log.warn("Fallo en refresco de token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED));
        }
    }
}
