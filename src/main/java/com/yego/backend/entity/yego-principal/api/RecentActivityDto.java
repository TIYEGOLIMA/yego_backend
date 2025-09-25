package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para actividad reciente del dashboard YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private Long id;
    private ActivityUserDto user;
    private String action;
    private String resource;
    private String resourceId;
    private String details;
    private LocalDateTime createdAt;
    private String ipAddress;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityUserDto {
        private Long id;
        private String username;
        private String name;
    }
}
