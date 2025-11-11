package com.backend.perfumes.services;

import com.backend.perfumes.model.*;
import com.backend.perfumes.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BrandRepository brandRepository,
                           CategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        /*if (userRepository.count() == 0) {
            User admin = new User();
            admin.setName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@perfumes.com");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);

            User vendedor = new User();
            vendedor.setName("Carlos");
            vendedor.setLastName("Vendedor");
            vendedor.setEmail("carlos@gmail.com");
            vendedor.setUsername("carlos");
            vendedor.setPassword(passwordEncoder.encode("password123"));
            vendedor.setRole(Role.VENDEDOR);
            vendedor.setActive(true);
            userRepository.save(vendedor);

            System.out.println("Usuarios de prueba creados:");
            System.out.println("Admin: admin@perfumes.com / password123");
            System.out.println("Vendedor: carlos@gmail.com / password123");
        }

        // Crear marcas de prueba
        if (brandRepository.count() == 0) {
            Brand chanel = new Brand();
            chanel.setName("Chanel");
            chanel.setDescription("Lujo francés");
            chanel.setCountryOrigin("Francia");
            brandRepository.save(chanel);

            Brand dior = new Brand();
            dior.setName("Dior");
            dior.setDescription("Alta costura");
            dior.setCountryOrigin("Francia");
            brandRepository.save(dior);

            System.out.println("Marcas de prueba creadas");
        }

        if (categoryRepository.count() == 0) {
            Category floral = new Category();
            floral.setName("Floral");
            floral.setDescription("Perfumes con notas florales");
            categoryRepository.save(floral);

            Category oriental = new Category();
            oriental.setName("Oriental");
            oriental.setDescription("Perfumes con notas orientales");
            categoryRepository.save(oriental);

            System.out.println("Categorías de prueba creadas");
        }*/
    }
}