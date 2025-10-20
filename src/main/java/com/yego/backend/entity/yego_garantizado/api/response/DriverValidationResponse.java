package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de validación de licencia de conductor
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverValidationResponse {
    private String licenseNumber;
    private boolean existe;
    private String mensaje;
    private DriverInfo driver;
}
