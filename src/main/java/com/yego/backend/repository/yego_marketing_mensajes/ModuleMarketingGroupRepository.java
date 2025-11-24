package com.yego.backend.repository.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.entities.ModuleMarketingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad ModuleMarketingGroup
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Repository
public interface ModuleMarketingGroupRepository extends JpaRepository<ModuleMarketingGroup, Long> {
    
    /**
     * Busca un grupo por su group_id
     * @param groupId ID del grupo de WhatsApp
     * @return Optional con el grupo encontrado
     */
    Optional<ModuleMarketingGroup> findByGroupId(String groupId);
    
    /**
     * Verifica si existe un grupo con el group_id dado
     * @param groupId ID del grupo de WhatsApp
     * @return true si existe, false en caso contrario
     */
    boolean existsByGroupId(String groupId);
}

