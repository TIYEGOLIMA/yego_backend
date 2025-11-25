package com.yego.backend.entity.yego_api_externo.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO para el endpoint GoBot/PPendientes
 * Contiene información sobre pagos pendientes de un conductor
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PPendientesResponse {
    
    private String nombre;
    private String flota;
    private Double monto;
    private Integer pagos;
    private String license;
    private String surnames;
    private String idcar;
    private String placa;
    private String iddriver;
    private String telefonop;
    private Long idyego;
    private String idflota;
    private Integer estatus;
    private String message;
    private String msystem;
}

