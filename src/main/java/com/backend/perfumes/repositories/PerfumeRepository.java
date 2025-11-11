package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Perfume;
import com.backend.perfumes.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Long> {

    @Query("SELECT p FROM Perfume p WHERE " +
            "(:filtro IS NULL OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    Page<Perfume> findByFiltro(@Param("filtro") String filtro, Pageable pageable);

    @Query("SELECT p FROM Perfume p WHERE p.user = :user AND " +
            "(:filtro IS NULL OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    Page<Perfume> findByUserAndFiltro(@Param("user") User user,
                                      @Param("filtro") String filtro,
                                      Pageable pageable);
}