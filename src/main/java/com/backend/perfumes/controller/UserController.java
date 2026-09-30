package com.backend.perfumes.controller;

import com.backend.perfumes.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Endpoints para gestion y seguridad de usuarios")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Solicitud de eliminacion de cuenta",
            description = "Envia un correo con un codigo para confirmar la eliminacion de la cuenta")
    @ApiResponse(responseCode = "200", description = "Correo de eliminacion enviado")
    @PostMapping("/request-delete")
    public ResponseEntity<?> requestDelete(@RequestParam("email") String email) {
        try {
            log.info("Solicitud de eliminacion de cuenta para: {}", email);
            String result = userService.requestDeletion(email);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            log.error("Error en solicitud de eliminacion: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Solicitud de cambio de correo",
            description = "Envia un correo con un codigo para confirmar el cambio de correo de la cuenta")
    @ApiResponse(responseCode = "200", description = "Correo de actualizacion enviado")
    @PostMapping("/request-update-email")
    public ResponseEntity<?> requestUpdate(@AuthenticationPrincipal UserDetails user,
                                           @RequestParam("newEmail") String newEmail) {
        try {
            log.info("Solicitud de cambio de correo para usuario: {} hacia: {}", user.getUsername(), newEmail);
            String msg = userService.requestChangeEmail(user.getUsername(), newEmail);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            log.error("Error en solicitud de cambio de correo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Confirmacion de eliminacion de cuenta",
            description = "Elimina la cuenta si el codigo de verificacion es correcto")
    @ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente")
    @PostMapping("/confirm-delete")
    public ResponseEntity<?> confirmDelete(
            @RequestParam("email") String email,
            @RequestParam("code") String code) {
        try {
            log.info("Confirmando eliminacion de cuenta para: {}", email);
            String result = userService.confirmDeleteUser(email, code);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            log.error("Error confirmando eliminacion: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Confirmacion de cambio de correo",
            description = "Verifica que el codigo sea correcto y actualiza el correo del usuario")
    @ApiResponse(responseCode = "200", description = "Correo actualizado correctamente")
    @PostMapping("/confirm-update-email")
    public ResponseEntity<?> confirmUpdate(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam("code") String code) {
        try {
            log.info("Confirmando actualizacion de correo para usuario: {}", user.getUsername());
            String msg = userService.confirmChangeEmail(user.getUsername(), code);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            log.error("Error confirmando actualizacion de correo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
