package com.yego.backend.entity.yego_pro_ops.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private Long userId;

    @Column(name = "user_id_modificado")
    private Long userIdModificado;

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

    @Column(name = "operacion_yape", length = 50)
    private String operacionYape;

    @Column(name = "adelanto", precision = 10, scale = 2)
    private BigDecimal adelanto;

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

    @Column(name = "monto_total_producido", precision = 12, scale = 2)
    private BigDecimal montoTotalProducido;

    @Column(name = "placa", length = 20)
    private String placa;

    @Column(name = "odometro_inicial")
    private Integer odometroInicial;

    @Column(name = "odometro_final")
    private Integer odometroFinal;

    @Column(name = "diferencia_odometro")
    private Integer diferenciaOdometro;

    @Column(name = "shift_session_id")
    private java.util.UUID shiftSessionId;

    // ── Mobile fields (nullable, web doesn't use them) ──

    @Column(name = "car_photos", columnDefinition = "TEXT")
    private String carPhotos;

    @Column(name = "selfie_uri", length = 500)
    private String selfieUri;

    @Column(name = "car_photos_cierre", columnDefinition = "TEXT")
    private String carPhotosCierre;

    @Column(name = "fotos_evidencia", columnDefinition = "TEXT")
    private String fotosEvidencia;

    @Column(name = "observaciones_apertura", columnDefinition = "TEXT")
    private String observacionesApertura;

    @Column(name = "observaciones_cierre", columnDefinition = "TEXT")
    private String observacionesCierre;

    @Column(name = "mantenimiento_requerido")
    private Boolean mantenimientoRequerido;

    @Column(name = "mantenimiento_descripcion", columnDefinition = "TEXT")
    private String mantenimientoDescripcion;

    @Column(name = "saldo_anterior", precision = 10, scale = 2)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_descripcion", length = 300)
    private String saldoDescripcion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

