package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta de creación de registro de garantizado
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroResponse {

    private Long yegId;
    private String yegLicenciaNumero;
    private String yegFlota;
    private Boolean yegTerminosAceptados;
    private LocalDateTime yegFechaRegistro;
    private LocalDateTime yegFechaModificacion;
    private String mensaje;
}
