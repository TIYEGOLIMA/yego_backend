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
public class SessionResponseDto {
    private Long id;
    private Long userId;
    private String tokenHash;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime expiresAt;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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
}
