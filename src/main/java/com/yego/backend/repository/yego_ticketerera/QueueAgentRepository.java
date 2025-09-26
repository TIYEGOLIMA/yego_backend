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
    
    // Buscar por usuario activo
    Optional<QueueAgent> findByUserIdAndIsActiveTrue(Long userId);
    
    // Buscar TODOS los registros por usuario activo (para manejar duplicados)
    List<QueueAgent> findAllByUserIdAndIsActiveTrue(Long userId);
    
    // Buscar por módulo de atención activo
    Optional<QueueAgent> findByModuleIdAndIsActiveTrue(Long moduleId);
    
    // Buscar todos los agentes activos
    List<QueueAgent> findByIsActiveTrue();
    
    // Verificar si un usuario tiene módulo asignado y está OCUPADO
    Optional<QueueAgent> findByUserIdAndStatusAndIsActiveTrue(Long userId, String status);
    
    // Verificar si un usuario tiene módulo asignado (cualquier estado)
    Optional<QueueAgent> findByUserIdAndModuleIdIsNotNullAndIsActiveTrue(Long userId);
}
