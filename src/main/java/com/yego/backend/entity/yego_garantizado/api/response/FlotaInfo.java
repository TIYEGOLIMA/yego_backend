package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información de una flota del conductor
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlotaInfo {
    private String parkId;
    private String parkName;
    private String city;
    private Boolean activo;
}
