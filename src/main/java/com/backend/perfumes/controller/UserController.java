package com.backend.perfumes.controller;

import com.backend.perfumes.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:8100")
@Tag(name = "Usuarios", description = "Endpoints para crud de usuarios")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Solicitud de eliminación de cuenta",
            description = "Envía un correo con un enlace para confirmar la eliminación de la cuenta")
    @ApiResponse(responseCode = "200", description = "Correo enviado")
    @PostMapping("/request-delete")
    public ResponseEntity<?> requestDeletion(@RequestParam("email") String email) {
        try {
            String result = userService.requestDeletion(email);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Solicitud de cambio de correo",
            description = "Envía un correo con un enlace para confirmar el cambio de correo de la cuenta")
    @ApiResponse(responseCode = "200", description = "Correo enviado")
    @PostMapping("/request-update-email")
    public ResponseEntity<?> requestUpdate(@AuthenticationPrincipal UserDetails user,
                                           @RequestParam("newEmail") String newEmail) {
        try {
            String msg = userService.requestChangeEmail(user.getUsername(), newEmail);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @Operation(summary = "Eliminacion de cuenta ", description = "elimina la cuenta si el token es correcto")
    @ApiResponse(responseCode = "202", description = "Usuario eliminado")
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestParam("token") String token) {

        try {
            String result = userService.verifyToken(token);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

    }
    @Operation(summary = "Confirmacion cambio de correo",
            description = "Verifica que el codigo sea correcto y cambia el correo del usuario ")
    @ApiResponse(responseCode = "200", description = "Correo actualizado correctamente")
    @PostMapping("/confirm-update-email")
    public ResponseEntity<?> confirmUpdate(@RequestParam("code") String code) {
        try {
            String msg = userService.confirmChangeEmail(code);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}
