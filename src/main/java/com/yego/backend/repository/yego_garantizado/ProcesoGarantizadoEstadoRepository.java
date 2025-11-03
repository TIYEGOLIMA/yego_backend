package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.ProcesoGarantizadoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad ProcesoGarantizadoEstado
 */
@Repository
public interface ProcesoGarantizadoEstadoRepository extends JpaRepository<ProcesoGarantizadoEstado, Long> {
    
    /**
     * Obtener el estado más reciente (solo debe haber uno)
     */
    Optional<ProcesoGarantizadoEstado> findFirstByOrderByIdDesc();
}

