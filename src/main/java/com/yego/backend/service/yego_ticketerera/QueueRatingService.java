package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearRatingRequest;

import java.util.List;

/**
 * Interface del servicio de QueueRating del sistema YEGO Ticketerera
 */
public interface QueueRatingService {
    
    QueueRating crearRating(CrearRatingRequest request);
    
    List<QueueRating> obtenerRatingsPorTicket(Long ticketId);
}
