package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad QueueAgent del sistema YEGO Ticketerera
 */
@Repository
public interface QueueAgentRepository extends JpaRepository<QueueAgent, Long> {
    
    Optional<QueueAgent> findByUserIdAndIsActiveTrue(Long userId);
    
    // Buscar por módulo de atención activo
    Optional<QueueAgent> findByModuleIdAndIsActiveTrue(Long moduleId);
    
    // Buscar todos los módulos ocupados (status OCUPADO y activos)
    List<QueueAgent> findByStatusAndIsActiveTrue(String status);
}
