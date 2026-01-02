package com.yego.backend.entity.yego_ticketerera.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de response para asignar módulo a usuario en el sistema YEGO Ticketerera
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarModuloResponse {
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("module_id")
    private Long moduleId;
    
    private String status;
}

