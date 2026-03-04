package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "module_calculated_shifts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculatedShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Column(name = "tipo_turno", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TipoTurno tipoTurno;

    @Column(name = "estado", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoTurno estado = EstadoTurno.activo;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;
    
    @Column(name = "cantidad_viajes")
    private Integer cantidadViajes; // Cantidad de viajes que tuvo el conductor en ese turno
    
    @Column(name = "monto_total")
    private Double montoTotal; // Liquidación: solo suma de efectivo (cash) a pagar al conductor
    
    @Column(name = "produccion_total")
    private Double produccionTotal; // Efectivo + pago corporativo + pago sin efectivo + propinas + compensación promoción + bonificación
    
    @Column(name = "comisiones_servicio")
    private Double comisionesServicio; // Solo comisiones del servicio (price_commission_service)
    
    @Column(name = "pagado", nullable = false)
    @Builder.Default
    private Boolean pagado = false; // Indica si ya se le pagó al conductor

    @Column(name = "es_manual", nullable = false)
    @Builder.Default
    private Boolean esManual = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("America/Lima"));
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now(ZoneId.of("America/Lima"));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneId.of("America/Lima"));
    }

    public enum TipoTurno {
        diurno, nocturno
    }

    public enum EstadoTurno {
        activo, finalizado
    }
}

