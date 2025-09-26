package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para métricas del dashboard YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalRoles;
    private Long totalPermissions;
    private Long totalImports;
    private Long activeSessions;
    private Long importsToday;
    private String importsChange;
    private Long errorCount;
}

