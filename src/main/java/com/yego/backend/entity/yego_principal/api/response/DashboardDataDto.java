package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para datos del dashboard YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataDto {
    private DashboardMetricsDto metrics;
    private List<RecentActivityDto> recentActivity;
    private SystemStatusDto systemStatus;
    private WeeklyStatsDto weeklyStats;
}

