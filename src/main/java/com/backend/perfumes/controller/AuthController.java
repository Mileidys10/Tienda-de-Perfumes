package com.backend.perfumes.controller;

import com.backend.perfumes.dto.LoginRequest;
import com.backend.perfumes.dto.RegiterDto;
import com.backend.perfumes.model.User;
import com.backend.perfumes.services.AuthService;
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

    @Autowired
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
            User usuario = authService.authenticate(request.getEmail(), request.getPassword());

            if (usuario.getRole() == null) {
                throw new RuntimeException("El usuario no tiene un rol asignado");
            }

            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", usuario.getRole());
            extraClaims.put("nombre", usuario.getName());
            extraClaims.put("email", usuario.getEmail());

            String jwtToken = jwtService.generateToken(extraClaims, usuario);

            return ResponseEntity.ok(AuthReponseBuilder.buildAuthResponse(jwtToken, usuario));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorReponseBuilder.buildErrorResponse(
                            e.getMessage(),
                            HttpStatus.UNAUTHORIZED
                    ));
        } catch (Exception e) {
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

            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", newUser.getRole());
            extraClaims.put("nombre", newUser.getName());
            extraClaims.put("email", newUser.getEmail());

            String token = jwtService.generateToken(extraClaims, newUser);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AuthReponseBuilder.buildAuthResponse(token, newUser));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorReponseBuilder.buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }
}