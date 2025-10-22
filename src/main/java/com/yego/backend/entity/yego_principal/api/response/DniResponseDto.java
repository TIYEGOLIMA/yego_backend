package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DniResponseDto {
    
    private boolean success;
    private String dni;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private String error;
}
