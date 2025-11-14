package com.backend.perfumes.services;

import com.backend.perfumes.dto.RegiterDto;
import com.backend.perfumes.model.Role;
import com.backend.perfumes.model.User;

import com.backend.perfumes.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private JwtService  jwtService;
    private EmailService emailService;


    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       EmailService emailService

                       ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    this.emailService = emailService;

    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!user.isActive()) {
            throw new RuntimeException("Debes verificar tu correo antes de iniciar sesión");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            return user;

        } catch (Exception e) {
            throw new RuntimeException("Credenciales inválidas");
        }
    }

    public User register(RegiterDto request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(request.getRole() != null ? Role.valueOf(request.getRole()) : Role.CLIENTE);
        newUser.setActive(false);

        newUser.setUsername(request.getEmail());

        User savedUser = userRepository.save(newUser);

        String token = jwtService.generateVerificationToken(savedUser);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return savedUser;

    }


    public String requestDeletion( String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateDeleteAccountToken(user);
        emailService.sendDeletionEmail(email, token);
        return "Solicitud de eliminar Correo enviada";
    }







    public String verifyAccount(String token) {
        if (!jwtService.isVerificationToken(token)) {
            throw new RuntimeException("Token inválido");
        }

        Claims claims = jwtService.extractAllClaims(token);
        Long userId = claims.get("id", Long.class);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setActive(true);
        userRepository.save(user);

        return "Cuenta verificada con éxito";
    }


public String verifyToken(String token) {
    if (!jwtService.isDeleteToken(token)) {
        throw new RuntimeException("Token invalido");

    }

    Claims claims = jwtService.extractAllClaims(token);
    Long userId = claims.get("id", Long.class);
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


    userRepository.delete(user);


        return "Usuario eliminado con exito";



}




}