// FavoritesRepository.java
package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Favorites;
import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritesRepository extends JpaRepository<Favorites, Long> {

    Optional<Favorites> findByUserAndPerfume(User user, Perfume perfume);

    List<Favorites> findByUserOrderByAddedAtDesc(User user);

    Page<Favorites> findByUserOrderByAddedAtDesc(User user, Pageable pageable);

    boolean existsByUserAndPerfume(User user, Perfume perfume);

    @Query("SELECT f.perfume FROM Favorites f WHERE f.user.id = :userId")
    Page<Perfume> findFavoritePerfumesByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByUser(User user);

    void deleteByUserAndPerfume(User user, Perfume perfume);
}