package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para estadísticas de auditoría del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatsDto {
    
    private Long totalLogs;
    private List<ActionStatsDto> actions;
    private List<ResourceStatsDto> resources;
    private List<UserStatsDto> users;
    private List<DailyStatsDto> dailyStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionStatsDto {
        private String action;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceStatsDto {
        private String resource;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatsDto {
        private Long userId;
        private String username;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatsDto {
        private String date;
        private Long count;
    }
}

