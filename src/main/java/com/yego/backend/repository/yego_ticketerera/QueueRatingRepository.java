package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QueueRatingRepository extends JpaRepository<QueueRating, Long> {

    @Query("SELECT qr FROM QueueRating qr ORDER BY qr.createdAt DESC")
    List<QueueRating> findRecentRatings(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT qr FROM QueueRating qr WHERE qr.ticketId IN :ticketIds")
    List<QueueRating> findByTicketIdIn(@Param("ticketIds") List<Long> ticketIds);

    @Query("SELECT AVG(qr.score) FROM QueueRating qr")
    Double getAverageRating();

    @Query("SELECT AVG(qr.score) FROM QueueRating qr WHERE qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin")
    Double getAverageRatingByDateRange(@Param("fechaInicio") LocalDateTime fechaInicio,
                                       @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT COUNT(qr) FROM QueueRating qr WHERE qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin")
    long countByCreatedAtBetween(@Param("fechaInicio") LocalDateTime fechaInicio,
                                 @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT qr FROM QueueRating qr WHERE qr.ticketId IN :ticketIds AND qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin")
    List<QueueRating> findByTicketIdInAndCreatedAtBetween(@Param("ticketIds") List<Long> ticketIds,
                                                          @Param("fechaInicio") LocalDateTime fechaInicio,
                                                          @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT qr FROM QueueRating qr WHERE qr.createdAt >= :fechaInicio AND qr.createdAt <= :fechaFin ORDER BY qr.createdAt DESC")
    List<QueueRating> findRecentRatingsByDateRange(org.springframework.data.domain.Pageable pageable,
                                                   @Param("fechaInicio") LocalDateTime fechaInicio,
                                                   @Param("fechaFin") LocalDateTime fechaFin);
}
