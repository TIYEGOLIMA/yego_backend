package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.CalculoGarantizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad CalculoGarantizado
 */
@Repository
public interface CalculoGarantizadoRepository extends JpaRepository<CalculoGarantizado, Long> {
    
    Optional<CalculoGarantizado> findByPaisAndCiudadAndSemana(String pais, String ciudad, String semana);
    
    List<CalculoGarantizado> findAllByPaisAndCiudadAndSemana(String pais, String ciudad, String semana);
    
    /**
     * Obtiene todas las combinaciones únicas de país y ciudad para una semana específica
     * @param semana Semana a buscar
     * @return Lista de arrays [pais, ciudad] únicos
     */
    @Query(value = "SELECT DISTINCT pais, ciudad FROM module_guaranteed_calculations WHERE semana = :semana AND activo = true", nativeQuery = true)
    List<Object[]> findDistinctPaisAndCiudadBySemana(@Param("semana") String semana);
}

