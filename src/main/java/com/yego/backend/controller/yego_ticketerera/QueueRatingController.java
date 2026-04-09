package com.yego.backend.controller.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueRating;
import com.yego.backend.entity.yego_ticketerera.api.request.CrearRatingRequest;
import com.yego.backend.service.yego_ticketerera.QueueRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de calificaciones del sistema YEGO Ticketerera
 */
@RestController
@RequestMapping("/api/ticketera/ratings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueueRatingController {
    
    private final QueueRatingService queueRatingService;
    
    @PostMapping
    public ResponseEntity<QueueRating> crearRating(@Valid @RequestBody CrearRatingRequest request) {
        QueueRating rating = queueRatingService.crearRating(request);
        return ResponseEntity.ok(rating);
    }
    
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<QueueRating>> obtenerRatingsPorTicket(@PathVariable Long ticketId) {
        List<QueueRating> ratings = queueRatingService.obtenerRatingsPorTicket(ticketId);
        return ResponseEntity.ok(ratings);
    }
}
