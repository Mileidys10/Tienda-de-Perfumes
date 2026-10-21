package com.backend.perfumes.services;

import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    private static final String USER_NOT_FOUND_MSG = "Usuario no encontrado: ";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Buscando usuario para autenticación: {}", username);

        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND_MSG + username));
        }

        log.debug("Usuario cargado: {}, Rol: {}, Activo: {}", user.getUsername(), user.getRole(), user.isActive());
        return user;
    }
}
