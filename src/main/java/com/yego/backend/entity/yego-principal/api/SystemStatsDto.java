package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDto {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalRoles;
    private Long totalPermissions;
    private Long totalImports;
    private Long activeSessions;
}
