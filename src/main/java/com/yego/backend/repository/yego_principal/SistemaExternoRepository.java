package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.SistemaExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para sistemas externos
 */
@Repository
public interface SistemaExternoRepository extends JpaRepository<SistemaExterno, Long> {
    
    /**
     * Buscar por nombre
     */
    Optional<SistemaExterno> findByYegoSisExtNombre(String nombre);
    
    /**
     * Buscar por URL
     */
    Optional<SistemaExterno> findByYegoSisExtUrl(String url);
    
    /**
     * Buscar por estado
     */
    List<SistemaExterno> findByYegoSisExtEstado(SistemaExterno.EstadoSistema estado);
    
    
    /**
     * Buscar sistemas activos
     */
    List<SistemaExterno> findByYegoSisExtActivoTrue();
    
    /**
     * Buscar por nombre o descripción (búsqueda parcial)
     */
    @Query("SELECT s FROM SistemaExterno s WHERE " +
           "LOWER(s.yegoSisExtNombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(s.yegoSisExtDescripcion) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<SistemaExterno> buscarPorTermino(String termino);
}
