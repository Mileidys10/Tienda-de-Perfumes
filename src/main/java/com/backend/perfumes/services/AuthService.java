package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException("Wrong password");


        }
    return user;
    }




    }

