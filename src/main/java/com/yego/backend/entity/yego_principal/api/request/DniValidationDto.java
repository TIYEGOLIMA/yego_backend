package com.yego.backend.entity.yego_principal.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para validación de DNI en YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DniValidationDto {
    private Boolean success;
    private String message;
    private DniDataDto data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DniDataDto {
        private String dni;
        private String nombres;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private String nombreCompleto;
        private String fechaNacimiento;
        private String sexo;
    }
}

