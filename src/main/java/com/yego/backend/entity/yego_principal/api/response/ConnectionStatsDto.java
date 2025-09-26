package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStatsDto {
    private Long totalConnections;
    private Long activeConnections;
    private Long failedConnections;
    private Double averageConnectionTime;
    private String mostCommonIpAddress;
    private String mostCommonUserAgent;
}

