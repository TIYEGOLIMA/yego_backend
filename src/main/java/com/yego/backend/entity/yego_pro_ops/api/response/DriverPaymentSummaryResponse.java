package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para el resumen de pagos de conductores
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverPaymentSummaryResponse {
    
    @JsonProperty("conductores")
    private List<ConductorPaymentInfo> conductores;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConductorPaymentInfo {
        @JsonProperty("driver_id")
        private String driverId;
        
        @JsonProperty("avatar_url")
        private String avatarUrl;
        
        @JsonProperty("nombre")
        private String nombre; // Nombre completo del conductor
        
        @JsonProperty("telefono")
        private String telefono; // Teléfono del conductor
        
        @JsonProperty("monto_total_pagar")
        private Double montoTotalPagar; // Suma de monto_total de turnos no pagados
        
        @JsonProperty("cantidad_turnos")
        private Integer cantidadTurnos; // Cantidad de turnos que tiene el conductor
        
        @JsonProperty("turnos")
        private List<TurnoInfo> turnos; // Lista de turnos del conductor
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnoInfo {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("fecha")
        private String fecha;
        
        @JsonProperty("hora_inicio")
        private String horaInicio;
        
        @JsonProperty("hora_fin")
        private String horaFin;
        
        @JsonProperty("tipo_turno")
        private String tipoTurno;
        
        @JsonProperty("duracion_minutos")
        private Integer duracionMinutos;
        
        @JsonProperty("monto_total")
        private Double montoTotal;
        
        @JsonProperty("pagado")
        private Boolean pagado;
    }
}

