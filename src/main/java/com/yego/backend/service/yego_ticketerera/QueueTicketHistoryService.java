package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.QueueTicketHistory;

import java.util.List;

/**
 * Servicio para gestionar el historial de tickets del sistema YEGO Ticketerera
 */
public interface QueueTicketHistoryService {
    
    /**
     * Registrar un cambio de estado de ticket
     */
    QueueTicketHistory registrarCambioEstado(
        Long ticketId,
        Long agentId,
        String previousStatus,
        String newStatus,
        String notes
    );
    
    /**
     * Obtener historial de un ticket
     */
    List<QueueTicketHistory> obtenerHistorialPorTicket(Long ticketId);
    
    /**
     * Obtener historial de un agente
     */
    List<QueueTicketHistory> obtenerHistorialPorAgente(Long agentId);
}