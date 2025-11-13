package com.backend.perfumes.repositories;


import com.backend.perfumes.model.RefreshToken;
import com.backend.perfumes.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :usuario")
    int deleteByUsuario(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteAllByExpiryDateBefore(Instant now);

    @Modifying
    //JPQL
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :usuarioId")
    void deleteByUsuarioId(Long usuarioId);
}