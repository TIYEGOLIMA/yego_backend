package com.yego.backend.entity.yego_ticketerera.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad QueueRating del sistema YEGO Ticketerera
 * Representa las calificaciones de los tickets atendidos
 */
@Entity
@Table(name = "queue_ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Column(name = "agent_id", nullable = false)
    private Long agentId;
    
    @Column(name = "score", nullable = false)
    private Integer score;
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Relación con Ticket (sin foreign key constraint para evitar problemas)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", insertable = false, updatable = false)
    private Ticket ticket;
}
