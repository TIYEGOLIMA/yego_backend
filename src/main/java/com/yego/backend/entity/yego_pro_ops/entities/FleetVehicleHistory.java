package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trazabilidad de cambios de flota (park) de un vehículo.
 * Tabla append-only: solo se insertan registros, nunca se actualizan ni borran.
 *   INGRESO      -> primera vez que el vehículo aparece (park de origen)
 *   CAMBIO_FLOTA -> el vehículo cambió de una flota a otra
 */
@Entity
@Table(name = "module_fleet_vehicle_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetVehicleHistory {

    public static final String TIPO_INGRESO = "INGRESO";
    public static final String TIPO_CAMBIO_FLOTA = "CAMBIO_FLOTA";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "yango_car_id", nullable = false, length = 255)
    private String yangoCarId;

    @Column(name = "number", length = 50)
    private String number;

    @Column(name = "segment_id_anterior")
    private UUID segmentIdAnterior;

    @Column(name = "segment_id_nuevo", nullable = false)
    private UUID segmentIdNuevo;

    @Column(name = "park_id_anterior", length = 255)
    private String parkIdAnterior;

    @Column(name = "park_id_nuevo", nullable = false, length = 255)
    private String parkIdNuevo;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
