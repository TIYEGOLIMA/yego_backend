package com.yego.backend.entity.yego_ticketerera.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entidad QueueTicketHistory del sistema YEGO Ticketerera
 * Representa el historial de cambios de estado de los tickets
 */
@Entity
@Table(name = "queue_ticket_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueTicketHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    
    @Column(name = "agent_id")
    private Long agentId; // Solo referencia, sin foreign key constraint
    
    @Column(name = "previous_status", length = 20)
    private String previousStatus;
    
    @Column(name = "new_status", length = 20, nullable = false)
    private String newStatus;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        // Usar zona horaria de Perú (America/Lima)
        this.createdAt = LocalDateTime.now(ZoneId.of("America/Lima"));
    }
}
