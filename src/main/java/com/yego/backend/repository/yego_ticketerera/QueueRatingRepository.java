package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la entidad QueueRating del sistema YEGO Ticketerera
 */
@Repository
public interface QueueRatingRepository extends JpaRepository<QueueRating, Long> {
    
    List<QueueRating> findByTicketId(Long ticketId);
    
    // Obtener calificaciones recientes (últimas N)
    @Query("SELECT qr FROM QueueRating qr ORDER BY qr.createdAt DESC")
    List<QueueRating> findRecentRatings(org.springframework.data.domain.Pageable pageable);
    
    // Consulta optimizada: Obtener calificaciones por múltiples tickets en una sola query
    @Query("SELECT qr FROM QueueRating qr WHERE qr.ticketId IN :ticketIds")
    List<QueueRating> findByTicketIdIn(@Param("ticketIds") List<Long> ticketIds);
    
    // Consulta optimizada: Obtener promedio general de todas las calificaciones
    @Query("SELECT AVG(qr.score) FROM QueueRating qr")
    Double getAverageRating();
    
    // Consulta optimizada: Obtener promedio general con filtro de fecha
    @Query("SELECT AVG(qr.score) FROM QueueRating qr WHERE qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin")
    Double getAverageRatingByDateRange(@Param("fechaInicio") java.time.LocalDateTime fechaInicio, 
                                       @Param("fechaFin") java.time.LocalDateTime fechaFin);
    
    // Consulta optimizada: Contar calificaciones con filtro de fecha
    @Query("SELECT COUNT(qr) FROM QueueRating qr WHERE qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin")
    long countByCreatedAtBetween(@Param("fechaInicio") java.time.LocalDateTime fechaInicio, 
                                 @Param("fechaFin") java.time.LocalDateTime fechaFin);
    
    // Consulta optimizada: Obtener calificaciones por múltiples tickets con filtro de fecha
    @Query("SELECT qr FROM QueueRating qr WHERE qr.ticketId IN :ticketIds AND qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin")
    List<QueueRating> findByTicketIdInAndCreatedAtBetween(@Param("ticketIds") List<Long> ticketIds,
                                                           @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
                                                           @Param("fechaFin") java.time.LocalDateTime fechaFin);
    
    // Consulta optimizada: Obtener calificaciones recientes con filtro de fecha
    @Query("SELECT qr FROM QueueRating qr WHERE qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin ORDER BY qr.createdAt DESC")
    List<QueueRating> findRecentRatingsByDateRange(org.springframework.data.domain.Pageable pageable,
                                                    @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
                                                    @Param("fechaFin") java.time.LocalDateTime fechaFin);
    
}
