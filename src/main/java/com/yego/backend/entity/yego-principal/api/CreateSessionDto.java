package com.yego.backend.entity.yego_principal.api;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionDto {
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;
    
    @NotNull(message = "El hash del token es obligatorio")
    private String tokenHash;
    
    private String ipAddress;
    private String userAgent;
    private LocalDateTime expiresAt;
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
