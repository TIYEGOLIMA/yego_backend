package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta de flotas del sistema YEGO Garantizado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlotaResponse {
    
    private String flotaId;
    private String flotaName;
    private String flotaCity;
    private List<String> flotaSpecifications;
}
