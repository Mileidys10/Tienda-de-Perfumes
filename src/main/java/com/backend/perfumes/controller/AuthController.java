package com.backend.perfumes.controller;

import com.backend.perfumes.dto.LoginRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticación", description = "Endpoints para login de usuarios ")
public class AuthController {

    @Autowired
    private AuthService authService;

    private final JwtService jwtService;

    public  AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Operation(
            summary = "Login para administradores",
            description = "Autentica usuarios ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login exitoso",
                            content = @Content(schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "400", description = "Credenciales inválidas")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        try{
            User usuario = authService.authenticate(request.getEmail(), request.getPassword());

            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("rol", usuario.getRole());
            extraClaims.put("nombre", usuario.getName());
            extraClaims.put("email", usuario.getEmail());

            String jwtToken = jwtService.generateToken(extraClaims, usuario);

            return ResponseEntity.ok(AuthReponseBuilder.buildAuthResponse(jwtToken, usuario));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorReponseBuilder.buildErrorResponse(
                            e.getMessage(),
                            HttpStatus.UNAUTHORIZED
                    ));
        }
    }
}
