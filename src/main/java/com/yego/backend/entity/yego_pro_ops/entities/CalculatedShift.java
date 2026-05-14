package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Integer cantidadViajes;

    @Column(name = "monto_total")
    private Double montoTotal;

    @Column(name = "produccion_total")
    private Double produccionTotal;

    @Column(name = "comisiones_servicio")
    private Double comisionesServicio;

    @Column(name = "placa", length = 20)
    private String placa;

    @Column(name = "pagado", nullable = false)
    @Builder.Default
    private Boolean pagado = false;

    @Column(name = "es_manual", nullable = false)
    @Builder.Default
    private Boolean esManual = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TipoTurno {
        manana, tarde
    }

    public enum EstadoTurno {
        activo, finalizado
    }
}

