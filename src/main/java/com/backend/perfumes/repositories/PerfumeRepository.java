package com.backend.perfumes.repositories;

import com.backend.perfumes.model.Perfume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Integer> {
    //nombre o genero
    @Query("""
    SELECT p FROM Perfume p
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :filtro, '%'))
    OR LOWER(p.genre) LIKE LOWER(CONCAT('%', :filtro, '%'))
            """)
    Page<Perfume> findByFiltro(@Param("filtro") String filtro, Pageable pageable);

}
