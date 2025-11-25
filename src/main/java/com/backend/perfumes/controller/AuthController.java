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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "1. Autenticación", description = "Endpoints para login de usuarios")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Autowired
    public AuthController(AuthService authService, JwtService jwtService, EmailService emailService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Operation(
            summary = "Login de usuarios (ADMIN, VENDEDOR, CLIENTE)",
            description = "Autentica un usuario y devuelve su token JWT junto a la información del perfil",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login exitoso",
                            content = @Content(schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o rol no autorizado",
                            content = @Content(schema = @Schema(implementation = Map.class)))
            }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            System.out.println("=== LOGIN REQUEST ===");
            System.out.println("Email: " + request.getEmail());

            User usuario = authService.authenticate(request.getEmail(), request.getPassword());

            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", usuario.getRole());
            extraClaims.put("nombre", usuario.getName());
            extraClaims.put("email", usuario.getEmail());

            String jwtToken = jwtService.generateToken(extraClaims, usuario);

            System.out.println("=== LOGIN SUCCESSFUL ===");
            System.out.println("Usuario: " + usuario.getUsername());
            System.out.println("Rol: " + usuario.getRole());

            return ResponseEntity.ok(AuthReponseBuilder.buildAuthResponse(jwtToken, usuario));

        } catch (RuntimeException e) {
            System.out.println("=== LOGIN FAILED ===");
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorReponseBuilder.buildErrorResponse(
                            e.getMessage(),
                            HttpStatus.UNAUTHORIZED
                    ));
        } catch (Exception e) {
            System.out.println("=== LOGIN ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorReponseBuilder.buildErrorResponse(
                            "Error interno del servidor",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    ));
        }
    }

    @Operation(summary = "Registro de usuarios", description = "Crea un nuevo usuario (ADMIN, VENDEDOR o CLIENTE)")
    @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegiterDto request) {
        try {
            User newUser = authService.register(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "status", "success",
                            "message", "Usuario registrado correctamente. Por favor verifica tu email antes de iniciar sesión.",
                            "data", Map.of(
                                    "id", newUser.getId(),
                                    "email", newUser.getEmail(),
                                    "username", newUser.getUsername()
                            )
                    ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @Operation(summary = "Verificación de correo", description = "Verifica la cuenta de usuario mediante token")
    @ApiResponse(responseCode = "200", description = "Cuenta verificada exitosamente")
    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam String email, @RequestParam String code) {
        try {
            String result = authService.verifyAccount(email,code);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Reenviar email de verificación", description = "Reenvía el email de verificación a un usuario")
    @ApiResponse(responseCode = "200", description = "Email reenviado exitosamente")
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        try {
            String result = authService.resendVerificationEmail(email);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }



    @Operation(summary = "Enviar email de prueba", description = "Endpoint para probar el servicio de email")
    @PostMapping("/send-test-email")
    public ResponseEntity<?> sendTestEmail() {
        try {
            emailService.sendVerificationEmail("test@example.com", "test-token-123");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Email de prueba enviado"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Error enviando email: " + e.getMessage()
            ));
        }
    }
}