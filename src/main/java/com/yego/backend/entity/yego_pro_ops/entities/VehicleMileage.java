package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_vehicle_mileage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMileage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "yango_car_id", nullable = false, length = 255)
    private String yangoCarId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "kilometraje", nullable = false, precision = 12, scale = 2)
    private BigDecimal kilometraje;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
