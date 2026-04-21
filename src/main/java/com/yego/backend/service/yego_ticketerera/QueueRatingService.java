package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearRatingRequest;

public interface QueueRatingService {

    QueueRating crearRating(CrearRatingRequest request);
}
