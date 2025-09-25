package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para estado del sistema YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDto {
    private String database;
    private String api;
    private String websockets;
    private String storage;
    private Double memory;
    private Double cpu;
    private Long uptime;
    private LocalDateTime lastCheck;
}
