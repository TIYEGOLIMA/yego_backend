package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.ModuloAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la entidad ModuloAtencion del sistema YEGO Ticketerera
 */
@Repository
public interface ModuloAtencionRepository extends JpaRepository<ModuloAtencion, Long> {
    
    List<ModuloAtencion> findByIsActiveTrueOrderByNameAsc();
}
