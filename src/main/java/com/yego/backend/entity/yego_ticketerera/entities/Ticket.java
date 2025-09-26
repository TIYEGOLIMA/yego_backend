package com.yego.backend.entity.yego_ticketerera.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entidad Ticket del sistema YEGO Ticketerera
 * Representa un ticket de atención al cliente
 */
@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ticket_number", unique = true, nullable = false)
    private String ticketNumber;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "option_id")
    private Long optionId;
    
    @Column(name = "module_id")
    private Long moduleId; // ID del módulo de atención
    
    @Column(name = "agent_id")
    private Long agentId; // ID del agente asignado
    
    @Column(name = "license_number")
    private String licenseNumber; // Número de licencia/teléfono
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.WAITING;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "called_at")
    private LocalDateTime calledAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = TicketStatus.WAITING;
        }
        if (priority == null) {
            priority = 1;
        }
    }
    
    public enum TicketStatus {
        WAITING, CALLED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
