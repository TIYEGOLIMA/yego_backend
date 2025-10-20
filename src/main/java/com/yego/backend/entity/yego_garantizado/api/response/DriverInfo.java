package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para información de conductores desde la tabla drivers
 * Solo para lectura, no se persiste
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverInfo {
    private String driverId;
    private String fullName;
    private String phone;
    private Double rating;
    private String licenseNumber;
}

