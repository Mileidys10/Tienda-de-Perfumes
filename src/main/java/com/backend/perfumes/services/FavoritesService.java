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

    private static final String USER_NOT_FOUND = "Usuario no encontrado";
    private static final String PERFUME_NOT_FOUND = "Perfume no encontrado";
    private static final String PERFUME_ALREADY_FAVORITE = "El perfume ya está en favoritos";
    private static final String ERROR_ADD_FAVORITE = "Error al agregar a favoritos: ";
    private static final String ERROR_REMOVE_FAVORITE = "Error al remover de favoritos: ";
    private static final String LOG_ADD_FAVORITE = "Perfume {} agregado a favoritos por usuario {}";
    private static final String LOG_REMOVE_FAVORITE = "Perfume {} removido de favoritos por usuario {}";
    private static final String LOG_ERROR_ADD = "Error agregando a favoritos: {}";
    private static final String LOG_ERROR_REMOVE = "Error removiendo de favoritos: {}";



    private User findByUsername(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));
    }

    private Perfume findById(Long perfumeId) {
        return perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new RuntimeException(PERFUME_NOT_FOUND ));
    }

    @Transactional
    public boolean addToFavorites(Long perfumeId, String username) {
        try {
            User user = findByUsername(username);

            Perfume perfume = findById(perfumeId);

            // Verificar si ya está en favoritos
            if (favoritesRepository.existsByUserAndPerfume(user, perfume)) {
                throw new RuntimeException(PERFUME_ALREADY_FAVORITE);
            }

            Favorites favorite = new Favorites();
            favorite.setUser(user);
            favorite.setPerfume(perfume);

            favoritesRepository.save(favorite);
            log.info(LOG_ADD_FAVORITE, perfumeId, username);
            return true;

        } catch (Exception e) {
            log.error(LOG_ERROR_ADD, e.getMessage());
            throw new RuntimeException(ERROR_ADD_FAVORITE + e.getMessage());
        }
    }

    @Transactional
    public boolean removeFromFavorites(Long perfumeId, String username) {
        try {
            User user = findByUsername(username);

            Perfume perfume = findById(perfumeId);

            favoritesRepository.deleteByUserAndPerfume(user, perfume);
            log.info( LOG_REMOVE_FAVORITE, perfumeId, username);
            return true;

        } catch (Exception e) {
            log.error(LOG_ERROR_REMOVE, e.getMessage());
            throw new RuntimeException(ERROR_REMOVE_FAVORITE + e.getMessage());
        }
    }

    public Page<Perfume> getUserFavorites(String username, Pageable pageable) {
        User user = findByUsername(username);

        return favoritesRepository.findFavoritePerfumesByUserId(user.getId(), pageable);
    }

    public boolean isFavorite(Long perfumeId, String username) {
        try {
            User user = findByUsername(username);

            Perfume perfume = findById(perfumeId);

            return favoritesRepository.existsByUserAndPerfume(user, perfume);

        } catch (Exception e) {
            return false;
        }
    }

    public long getFavoriteCount(String username) {
        User user = findByUsername(username);

        return favoritesRepository.countByUser(user);
    }
}