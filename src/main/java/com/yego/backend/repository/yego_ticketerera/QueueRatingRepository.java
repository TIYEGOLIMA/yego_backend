package com.yego.backend.repository.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la entidad QueueRating del sistema YEGO Ticketerera
 */
@Repository
public interface QueueRatingRepository extends JpaRepository<QueueRating, Long> {
    
    List<QueueRating> findByTicketId(Long ticketId);
}
