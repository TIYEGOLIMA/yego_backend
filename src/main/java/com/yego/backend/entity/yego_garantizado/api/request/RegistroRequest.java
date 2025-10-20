package com.yego.backend.entity.yego_garantizado.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para crear un registro de garantizado
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequest {

    @NotBlank(message = "El número de licencia es requerido")
    private String yegLicenciaNumero;

    @NotBlank(message = "La flota es requerida")
    private String yegFlota;

    @NotNull(message = "Debe aceptar los términos y condiciones")
    private Boolean yegTerminosAceptados;
}
