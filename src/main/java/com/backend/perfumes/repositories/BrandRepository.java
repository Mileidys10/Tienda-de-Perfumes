package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Brand;
import com.backend.perfumes.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByUser(User user);

    Optional<Brand> findByIdAndUser(Long id, User user);

    @Query("SELECT b FROM Brand b WHERE b.user = :user AND " +
            "(:filtro IS NULL OR " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    List<Brand> findByUserAndFiltro(@Param("user") User user, @Param("filtro") String filtro);

    boolean existsByName(String name);
}