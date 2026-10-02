package com.backend.perfumes.services;

import com.backend.perfumes.model.*;
import com.backend.perfumes.repositories.BrandRepository;
import com.backend.perfumes.repositories.CategoryRepository;
import com.backend.perfumes.repositories.PerfumeRepository;
import com.backend.perfumes.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final PerfumeRepository perfumeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando verificación de datos iniciales en DataInitializer...");

        // 1. Crear usuarios esenciales si no existen
        User adminUser = userRepository.findByEmail("admin@perfumes.com").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setName("Administrador");
            admin.setLastName("Haute Parfumerie");
            admin.setEmail("admin@perfumes.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setEmailVerified(true);
            return userRepository.save(admin);
        });

        User sellerUser = userRepository.findByEmail("seller@perfumes.com").orElseGet(() -> {
            User seller = new User();
            seller.setUsername("seller");
            seller.setName("Maestro");
            seller.setLastName("Perfumista");
            seller.setEmail("seller@perfumes.com");
            seller.setPassword(passwordEncoder.encode("Seller123!"));
            seller.setRole(Role.VENDEDOR);
            seller.setActive(true);
            seller.setEmailVerified(true);
            return userRepository.save(seller);
        });

        User clientUser = userRepository.findByEmail("client@perfumes.com").orElseGet(() -> {
            User client = new User();
            client.setUsername("client");
            client.setName("Coleccionista");
            client.setLastName("Sibarita");
            client.setEmail("client@perfumes.com");
            client.setPassword(passwordEncoder.encode("Client123!"));
            client.setRole(Role.CLIENTE);
            client.setActive(true);
            client.setEmailVerified(true);
            return userRepository.save(client);
        });

        // 2. Si no hay marcas, inicializar catálogo noble
        if (brandRepository.count() == 0) {
            log.info("Inicializando marcas, categorías y fragancias de lujo...");

            // Categorías
            Category catOriental = new Category();
            catOriental.setName("Oriental & Oud");
            catOriental.setDescription("Acordes ambarinos profundos, inciensos y maderas místicas.");
            catOriental.setModerationStatus(ModerationStatus.APPROVED);
            categoryRepository.save(catOriental);

            Category catAmaderado = new Category();
            catAmaderado.setName("Amaderado & Aristocrático");
            catAmaderado.setDescription("Sándalo sagrado, cedro del Atlas y maderas nobles.");
            catAmaderado.setModerationStatus(ModerationStatus.APPROVED);
            categoryRepository.save(catAmaderado);

            Category catCitrico = new Category();
            catCitrico.setName("Cítrico & Fresco");
            catCitrico.setDescription("Bergamota de Calabria, flor de azahar y notas marinas luminosas.");
            catCitrico.setModerationStatus(ModerationStatus.APPROVED);
            categoryRepository.save(catCitrico);

            Category catFloral = new Category();
            catFloral.setName("Floral & Gourmand");
            catFloral.setDescription("Rosa de mayo de Grasse, jazmín Sambac y toques de vainilla Bourbon.");
            catFloral.setModerationStatus(ModerationStatus.APPROVED);
            categoryRepository.save(catFloral);

            // Marcas
            Brand brandTomFord = new Brand();
            brandTomFord.setName("Tom Ford Private Blend");
            brandTomFord.setDescription("Creaciones audaces, seductoras y de máxima opulencia.");
            brandTomFord.setCountryOrigin("Estados Unidos");
            brandTomFord.setImageUrl("https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=400&q=80");
            brandTomFord.setModerationStatus(ModerationStatus.APPROVED);
            brandTomFord.setUser(sellerUser);
            brandRepository.save(brandTomFord);

            Brand brandMFK = new Brand();
            brandMFK.setName("Maison Francis Kurkdjian");
            brandMFK.setDescription("Perfección y poesía aromática desde París.");
            brandMFK.setCountryOrigin("Francia");
            brandMFK.setImageUrl("https://images.unsplash.com/photo-1547887537-6158d64c35b3?auto=format&fit=crop&w=400&q=80");
            brandMFK.setModerationStatus(ModerationStatus.APPROVED);
            brandMFK.setUser(sellerUser);
            brandRepository.save(brandMFK);

            Brand brandCreed = new Brand();
            brandCreed.setName("House of Creed");
            brandCreed.setDescription("Dinastía real de alta perfumería artesanal desde 1760.");
            brandCreed.setCountryOrigin("Reino Unido");
            brandCreed.setImageUrl("https://images.unsplash.com/photo-1523293182086-7651a899d37f?auto=format&fit=crop&w=400&q=80");
            brandCreed.setModerationStatus(ModerationStatus.APPROVED);
            brandCreed.setUser(sellerUser);
            brandRepository.save(brandCreed);

            Brand brandChanel = new Brand();
            brandChanel.setName("Chanel Les Exclusifs");
            brandChanel.setDescription("El pináculo de la elegancia atemporal de la alta costura.");
            brandChanel.setCountryOrigin("Francia");
            brandChanel.setImageUrl("https://images.unsplash.com/photo-1588405748880-12d1d2a59f75?auto=format&fit=crop&w=400&q=80");
            brandChanel.setModerationStatus(ModerationStatus.APPROVED);
            brandChanel.setUser(sellerUser);
            brandRepository.save(brandChanel);

            // Perfumes Aprobados
            Perfume p1 = new Perfume();
            p1.setName("Oud Wood Extrait");
            p1.setDescription("Un acorde magistral de madera de oud, palo de rosa, cardamomo y ámbar líquido.");
            p1.setPrice(295.00);
            p1.setStock(18);
            p1.setSizeMl(100);
            p1.setGenre(Genre.Unisex);
            p1.setReleaseDate(LocalDate.of(2023, 5, 10));
            p1.setImageUrl("https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=600&q=80");
            p1.setModerationStatus(ModerationStatus.APPROVED);
            p1.setBrand(brandTomFord);
            p1.setCategory(catOriental);
            p1.setUser(sellerUser);
            p1.setFeatured(true);
            perfumeRepository.save(p1);

            Perfume p2 = new Perfume();
            p2.setName("Baccarat Rouge 540");
            p2.setDescription("Aura radiante con jazmín de Egipto, azafrán resplandeciente y cedro recién cortado.");
            p2.setPrice(325.00);
            p2.setStock(12);
            p2.setSizeMl(70);
            p2.setGenre(Genre.Unisex);
            p2.setReleaseDate(LocalDate.of(2022, 11, 15));
            p2.setImageUrl("https://images.unsplash.com/photo-1547887537-6158d64c35b3?auto=format&fit=crop&w=600&q=80");
            p2.setModerationStatus(ModerationStatus.APPROVED);
            p2.setBrand(brandMFK);
            p2.setCategory(catOriental);
            p2.setUser(sellerUser);
            p2.setFeatured(true);
            perfumeRepository.save(p2);

            Perfume p3 = new Perfume();
            p3.setName("Aventus Royal Imperial");
            p3.setDescription("Una estela legendaria de piña ahumada, abedul de Luisiana, grosella negra y musgo de roble.");
            p3.setPrice(365.00);
            p3.setStock(9);
            p3.setSizeMl(100);
            p3.setGenre(Genre.Masculino);
            p3.setReleaseDate(LocalDate.of(2021, 3, 20));
            p3.setImageUrl("https://images.unsplash.com/photo-1523293182086-7651a899d37f?auto=format&fit=crop&w=600&q=80");
            p3.setModerationStatus(ModerationStatus.APPROVED);
            p3.setBrand(brandCreed);
            p3.setCategory(catAmaderado);
            p3.setUser(sellerUser);
            p3.setFeatured(true);
            perfumeRepository.save(p3);

            Perfume p4 = new Perfume();
            p4.setName("Bleu de Chanel Parfum");
            p4.setDescription("Nobleza aromática y profundidad amaderada de sándalo de Nueva Caledonia con limón primofiore.");
            p4.setPrice(185.00);
            p4.setStock(24);
            p4.setSizeMl(100);
            p4.setGenre(Genre.Masculino);
            p4.setReleaseDate(LocalDate.of(2023, 8, 5));
            p4.setImageUrl("https://images.unsplash.com/photo-1588405748880-12d1d2a59f75?auto=format&fit=crop&w=600&q=80");
            p4.setModerationStatus(ModerationStatus.APPROVED);
            p4.setBrand(brandChanel);
            p4.setCategory(catAmaderado);
            p4.setUser(sellerUser);
            p4.setFeatured(false);
            perfumeRepository.save(p4);

            Perfume p5 = new Perfume();
            p5.setName("Tobacco Vanille Private");
            p5.setDescription("Opulencia clásica de hojas de tabaco de Virginia, haba tonka tostada, cacao y flor de tabaco.");
            p5.setPrice(285.00);
            p5.setStock(15);
            p5.setSizeMl(100);
            p5.setGenre(Genre.Unisex);
            p5.setReleaseDate(LocalDate.of(2022, 10, 1));
            p5.setImageUrl("https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=600&q=80");
            p5.setModerationStatus(ModerationStatus.APPROVED);
            p5.setBrand(brandTomFord);
            p5.setCategory(catFloral);
            p5.setUser(sellerUser);
            p5.setFeatured(true);
            perfumeRepository.save(p5);

            Perfume p6 = new Perfume();
            p6.setName("Grand Soir Parisien");
            p6.setDescription("El resplandor de una noche inolvidable en París con benjuí de Siam y ámbar dorado.");
            p6.setPrice(240.00);
            p6.setStock(14);
            p6.setSizeMl(70);
            p6.setGenre(Genre.Unisex);
            p6.setReleaseDate(LocalDate.of(2023, 1, 12));
            p6.setImageUrl("https://images.unsplash.com/photo-1547887537-6158d64c35b3?auto=format&fit=crop&w=600&q=80");
            p6.setModerationStatus(ModerationStatus.APPROVED);
            p6.setBrand(brandMFK);
            p6.setCategory(catOriental);
            p6.setUser(sellerUser);
            p6.setFeatured(false);
            perfumeRepository.save(p6);

            Perfume p7 = new Perfume();
            p7.setName("Silver Mountain Water");
            p7.setDescription("La pureza cristalina de los arroyos alpinos con grosella negra, té verde y gálbano.");
            p7.setPrice(310.00);
            p7.setStock(11);
            p7.setSizeMl(100);
            p7.setGenre(Genre.Unisex);
            p7.setReleaseDate(LocalDate.of(2022, 6, 18));
            p7.setImageUrl("https://images.unsplash.com/photo-1523293182086-7651a899d37f?auto=format&fit=crop&w=600&q=80");
            p7.setModerationStatus(ModerationStatus.APPROVED);
            p7.setBrand(brandCreed);
            p7.setCategory(catCitrico);
            p7.setUser(sellerUser);
            p7.setFeatured(false);
            perfumeRepository.save(p7);

            log.info("Catálogo inicial noble sembrado con éxito (4 marcas, 4 categorías, 7 fragancias de lujo).");
        } else {
            log.info("El catálogo ya contiene marcas registradas. No se requiere siembra.");
        }
    }
}
