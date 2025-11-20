package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== UserDetailService ===");
        System.out.println("Buscando usuario por: " + username);

        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        }

        System.out.println("Usuario encontrado: " + user.getUsername());
        System.out.println("Rol: " + user.getRole());
        System.out.println("Authorities: " + user.getAuthorities());
        System.out.println("Email verificado: " + user.isEmailVerified());
        System.out.println("Activo: " + user.isActive());
        System.out.println("=== Fin UserDetailService ===");

        return user;
    }
}