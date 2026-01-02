package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_driver_closes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverClose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Usuario que registró el cierre

    @Column(name = "user_id_modificado")
    private Long userIdModificado; // Usuario que modificó el cierre

    @Column(name = "gnv_m3", length = 50)
    private String gnvM3;

    @Column(name = "gnv_soles", precision = 10, scale = 2)
    private BigDecimal gnvSoles;

    @Column(name = "gasolina_galones", length = 50)
    private String gasolinaGalones;

    @Column(name = "gasolina_soles", precision = 10, scale = 2)
    private BigDecimal gasolinaSoles;

    @Column(name = "liquida_efectivo", precision = 10, scale = 2)
    private BigDecimal liquidaEfectivo;

    @Column(name = "liquida_yape", precision = 10, scale = 2)
    private BigDecimal liquidaYape;

    @Column(name = "otros_gastos", precision = 10, scale = 2)
    private BigDecimal otrosGastos;

    @Column(name = "otros_gastos_descripcion", columnDefinition = "TEXT")
    private String otrosGastosDescripcion;

    @Column(name = "total_ingresos", precision = 10, scale = 2)
    private BigDecimal totalIngresos;

    @Column(name = "total_gastos", precision = 10, scale = 2)
    private BigDecimal totalGastos;

    @Column(name = "resta", precision = 10, scale = 2)
    private BigDecimal resta;

    @Column(name = "calculated_shift_ids", length = 255)
    private String calculatedShiftIds; // IDs de CalculatedShift separados por coma (ej: "1,2" para identificar los registros de turnos)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

