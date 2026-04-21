package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearRatingRequest;
import com.yego.backend.service.yego_ticketerera.QueueRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticketera/ratings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueueRatingController {

    private final QueueRatingService queueRatingService;

    @PostMapping
    public ResponseEntity<QueueRating> crearRating(@Valid @RequestBody CrearRatingRequest request) {
        return ResponseEntity.ok(queueRatingService.crearRating(request));
    }
}
