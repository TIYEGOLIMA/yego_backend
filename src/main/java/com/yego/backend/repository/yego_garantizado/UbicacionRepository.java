package com.yego.backend.repository.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.entities.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Ubicacion
 */
@Repository
public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {
    
    List<Ubicacion> findByNivelAndActivoTrue(String nivel);
    
    List<Ubicacion> findByParentIdAndActivoTrue(Long parentId);
    
    List<Ubicacion> findByParentIdAndNivelAndActivoTrue(Long parentId, String nivel);
}

