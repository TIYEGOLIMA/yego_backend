package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para la tabla garantizado_registro
 * Maneja los registros de conductores garantizados
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Entity
@Table(name = "garantizado_registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yeg_id")
    private Long yegId;

    @Column(name = "yeg_licencia_numero", nullable = false)
    private String yegLicenciaNumero;

    @Column(name = "yeg_flota", nullable = false)
    private String yegFlota;

    @Column(name = "yeg_semana", nullable = false)
    private String yegSemana;

    @Column(name = "yeg_terminos_aceptados", nullable = false)
    private Boolean yegTerminosAceptados;

    @Column(name = "yeg_fecha_registro", nullable = false, updatable = false)
    private LocalDateTime yegFechaRegistro;

    @Column(name = "yeg_fecha_modificacion", nullable = false)
    private LocalDateTime yegFechaModificacion;

    @PrePersist
    protected void onCreate() {
        yegFechaRegistro = LocalDateTime.now();
        yegFechaModificacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        yegFechaModificacion = LocalDateTime.now();
    }
}

