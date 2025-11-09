package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Role;
import com.backend.perfumes.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest  // Levanta solo la capa JPA (rápido, con BD en memoria por defecto)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findUserByUsername() {
        // Crear usuario de prueba
        User testUser = new User(
                "Test",
                "User",
                "test@perfumes.com",
                "123",
                Role.CLIENTE
        );

        // Guardar en BD
        User savedUser = userRepository.save(testUser);

        // Buscar por email
        Optional<User> encontradoOptional = userRepository.findByEmail(savedUser.getEmail());

        assertTrue(encontradoOptional.isPresent(), "The user must be in the database");

        User foundUser = encontradoOptional.get();

        assertEquals("test@perfumes.com", foundUser.getEmail(), "The user must match with the testing one");
    }
}
