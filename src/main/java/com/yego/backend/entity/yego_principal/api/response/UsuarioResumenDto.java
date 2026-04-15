package com.yego.backend.entity.yego_principal.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resumen de usuario para listado: usuario, rol, esJefe, área, nombre, apellido, email.
 * Sin paginación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResumenDto {

    private Long id;
    @JsonProperty("usuario")
    private String username;
    @JsonProperty("rol")
    private String rol;
    @JsonProperty("esJefe")
    private Boolean esJefe;
    @JsonProperty("esSupervisor")
    private Boolean esSupervisor;
    @JsonProperty("area")
    private String area;
    @JsonProperty("nombre")
    private String nombre;
    @JsonProperty("apellido")
    private String apellido;
    @JsonProperty("email")
    private String email;
    @JsonProperty("dni")
    private String dni;
}
