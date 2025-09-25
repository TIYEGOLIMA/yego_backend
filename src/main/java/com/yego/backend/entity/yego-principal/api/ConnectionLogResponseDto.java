package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionLogResponseDto {
    private Long id;
    private Long userId;
    private Long sessionId;
    private String action;
    private String ipAddress;
    private String userAgent;
    private String device;
    private String browser;
    private String operatingSystem;
    private String city;
    private String region;
    private String country;
    private String countryCode;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private String isp;
    private String organization;
    private Integer sessionDuration;
    private String roleName;
    private LocalDateTime createdAt;
}
