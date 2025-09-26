package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatsDto {
    private Long totalSessions;
    private Long activeSessions;
    private Long inactiveSessions;
    private Double averageSessionDuration;
    private Long totalUsers;
    private Long uniqueUsers;
    
    // Campos adicionales para compatibilidad
    private Long totalActive;
    private Long totalCreatedToday;
}

