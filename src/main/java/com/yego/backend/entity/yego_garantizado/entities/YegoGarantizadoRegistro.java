package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "garantizado_registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YegoGarantizadoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yeg_id")
    private Long id;

    @Column(name = "yeg_fecha_registro", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @Column(name = "yeg_fecha_modificacion")
    @UpdateTimestamp
    private LocalDateTime fechaModificacion;

    @Column(name = "yeg_flota", nullable = false)
    private String flota;

    @Column(name = "yeg_licencia_numero", nullable = false)
    private String licenciaNumero;

    @Column(name = "yeg_terminos_aceptados", nullable = false)
    private Boolean terminosAceptados;

    @Column(name = "yeg_semana", nullable = false)
    private String semana;


}