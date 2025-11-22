package com.backend.perfumes.repositories;

import com.backend.perfumes.model.ModerationStatus;
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
public interface PerfumeRepository extends JpaRepository<Perfume, Long> {

    @Query("SELECT p FROM Perfume p WHERE " +
            "(:filtro IS NULL OR " +
            "CAST(p.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.description AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.brand.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%'))")
    Page<Perfume> findByFiltro(@Param("filtro") String filtro, Pageable pageable);

    @Query("SELECT p FROM Perfume p WHERE p.moderationStatus = :status AND " +
            "(:filtro IS NULL OR " +
            "CAST(p.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.description AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.brand.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%'))")
    Page<Perfume> findByModerationStatusAndFiltro(@Param("status") ModerationStatus status,
                                                  @Param("filtro") String filtro, Pageable pageable);

    @Query("SELECT p FROM Perfume p WHERE p.user = :user AND " +
            "(:filtro IS NULL OR " +
            "CAST(p.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.description AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.brand.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%'))")
    Page<Perfume> findByUserAndFiltro(@Param("user") User user,
                                      @Param("filtro") String filtro,
                                      Pageable pageable);

    @Query("SELECT p FROM Perfume p WHERE p.brand.id = :brandId AND p.user = :user AND " +
            "(:filtro IS NULL OR " +
            "CAST(p.name AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%') OR " +
            "CAST(p.description AS string) LIKE CONCAT('%', CAST(:filtro AS string), '%'))")
    List<Perfume> findByBrandAndUserWithFiltro(@Param("brandId") Long brandId,
                                               @Param("user") User user,
                                               @Param("filtro") String filtro);

    List<Perfume> findByBrandIdAndUser(Long brandId, User user);
    List<Perfume> findByBrandIdAndModerationStatus(Long brandId, ModerationStatus status);

    Optional<Perfume> findByIdAndModerationStatus(Long id, ModerationStatus status);
    List<Perfume> findByModerationStatus(ModerationStatus status);
    Page<Perfume> findByModerationStatus(ModerationStatus status, Pageable pageable);

    long countByUser(User user);
    long countByUserAndModerationStatus(User user, ModerationStatus status);
}