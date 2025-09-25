package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad Import del sistema YEGO Principal
 */
@Repository
public interface ImportRepository extends JpaRepository<Import, Long> {
    
    /**
     * Buscar importaciones por usuario
     */
    List<Import> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Buscar importaciones por rango de fechas
     */
    @Query("SELECT i FROM Import i WHERE i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
    List<Import> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Buscar importaciones por usuario y rango de fechas
     */
    @Query("SELECT i FROM Import i WHERE i.userId = :userId AND i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
    List<Import> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Buscar importaciones por usuario desde una fecha
     */
    @Query("SELECT i FROM Import i WHERE i.userId = :userId AND i.createdAt >= :startDate ORDER BY i.createdAt DESC")
    List<Import> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );
    
    /**
     * Buscar importaciones desde una fecha
     */
    @Query("SELECT i FROM Import i WHERE i.createdAt >= :startDate ORDER BY i.createdAt DESC")
    List<Import> findByCreatedAtAfterOrderByCreatedAtDesc(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Buscar importaciones hasta una fecha
     */
    @Query("SELECT i FROM Import i WHERE i.createdAt <= :endDate ORDER BY i.createdAt DESC")
    List<Import> findByCreatedAtBeforeOrderByCreatedAtDesc(@Param("endDate") LocalDateTime endDate);
    
    /**
     * Buscar importaciones por usuario hasta una fecha
     */
    @Query("SELECT i FROM Import i WHERE i.userId = :userId AND i.createdAt <= :endDate ORDER BY i.createdAt DESC")
    List<Import> findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Contar importaciones por estado
     */
    Long countByStatus(Import.ImportStatus status);
    
    /**
     * Contar importaciones por tipo
     */
    Long countByType(Import.ImportType type);
    
    /**
     * Buscar todas las importaciones ordenadas por fecha de creación descendente
     */
    List<Import> findAllByOrderByCreatedAtDesc();
    
    /**
     * Contar importaciones después de una fecha
     */
    Long countByCreatedAtAfter(LocalDateTime date);
}
