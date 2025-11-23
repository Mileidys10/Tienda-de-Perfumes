package com.backend.perfumes.services;

import com.backend.perfumes.model.Favorites;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.model.User;
import com.backend.perfumes.repositories.FavoritesRepository;
import com.backend.perfumes.repositories.PerfumeRepository;
import com.backend.perfumes.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoritesService {

    private final FavoritesRepository favoritesRepository;
    private final PerfumeRepository perfumeRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean addToFavorites(Long perfumeId, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Perfume perfume = perfumeRepository.findById(perfumeId)
                    .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));

            // Verificar si ya está en favoritos
            if (favoritesRepository.existsByUserAndPerfume(user, perfume)) {
                throw new RuntimeException("El perfume ya está en favoritos");
            }

            Favorites favorite = new Favorites();
            favorite.setUser(user);
            favorite.setPerfume(perfume);

            favoritesRepository.save(favorite);
            log.info("Perfume {} agregado a favoritos por usuario {}", perfumeId, username);
            return true;

        } catch (Exception e) {
            log.error("Error agregando a favoritos: {}", e.getMessage());
            throw new RuntimeException("Error al agregar a favoritos: " + e.getMessage());
        }
    }

    @Transactional
    public boolean removeFromFavorites(Long perfumeId, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Perfume perfume = perfumeRepository.findById(perfumeId)
                    .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));

            favoritesRepository.deleteByUserAndPerfume(user, perfume);
            log.info("Perfume {} removido de favoritos por usuario {}", perfumeId, username);
            return true;

        } catch (Exception e) {
            log.error("Error removiendo de favoritos: {}", e.getMessage());
            throw new RuntimeException("Error al remover de favoritos: " + e.getMessage());
        }
    }

    public Page<Perfume> getUserFavorites(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return favoritesRepository.findFavoritePerfumesByUserId(user.getId(), pageable);
    }

    public boolean isFavorite(Long perfumeId, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Perfume perfume = perfumeRepository.findById(perfumeId)
                    .orElseThrow(() -> new RuntimeException("Perfume no encontrado"));

            return favoritesRepository.existsByUserAndPerfume(user, perfume);

        } catch (Exception e) {
            return false;
        }
    }

    public long getFavoriteCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return favoritesRepository.countByUser(user);
    }
}