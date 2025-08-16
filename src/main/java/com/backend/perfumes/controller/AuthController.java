package com.backend.perfumes.controller;

import com.backend.perfumes.dto.LoginRequest;
import com.backend.perfumes.model.User;
import com.backend.perfumes.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity <?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            User user = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

            return ResponseEntity.ok(Map.of(
                    "Message", " Succes Login",
                    "data", Map.of(
                            "email ", user.getEmail(),
                            "Rol", user.getRole()
                    )


            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "Error" , "Credenciales incorrectas"
            ));
        }
    };
}
