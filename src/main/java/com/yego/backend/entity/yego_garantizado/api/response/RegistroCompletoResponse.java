package com.yego.backend.entity.yego_garantizado.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta simplificado para registros
 * Solo incluye: licencia, hora de registro, yegoFlota, nombre de flota y semana
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCompletoResponse {
    
    // Solo los campos solicitados
    private String yegLicenciaNumero;        // Licencia
    private LocalDateTime yegFechaRegistro;  // Hora de registro
    private String yegFlota;                 // yegoFlota
    private String flotaNombre;              // Nombre de la flota
    private String yegSemana;                // Semana
}
