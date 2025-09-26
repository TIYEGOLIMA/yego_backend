package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Configuration del sistema YEGO Principal
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    
    /**
     * Buscar configuración por clave
     */
    Optional<Configuration> findByKey(String key);
    
    /**
     * Verificar si existe una configuración por clave
     */
    boolean existsByKey(String key);
    
    /**
     * Buscar configuraciones por categoría
     */
    List<Configuration> findByCategoryOrderByKeyAsc(String category);
    
    /**
     * Buscar todas las configuraciones ordenadas por categoría y clave
     */
    List<Configuration> findAllByOrderByCategoryAscKeyAsc();
    
    /**
     * Obtener categorías distintas
     */
    @Query("SELECT DISTINCT c.category FROM Configuration c WHERE c.category IS NOT NULL ORDER BY c.category ASC")
    List<String> findDistinctCategories();
    
    /**
     * Buscar configuraciones por tipo
     */
    List<Configuration> findByTypeOrderByKeyAsc(String type);
    
    /**
     * Buscar configuraciones que contengan una palabra clave en la descripción
     */
    @Query("SELECT c FROM Configuration c WHERE c.description LIKE %:keyword% ORDER BY c.category ASC, c.key ASC")
    List<Configuration> findByDescriptionContainingIgnoreCase(String keyword);
    
    /**
     * Contar configuraciones por categoría
     */
    Long countByCategory(String category);
}

