package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.CalculoGarantizado;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

