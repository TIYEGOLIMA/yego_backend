package com.yego.backend.entity.yego_principal.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_grupos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grupo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yeg_gru_id")
    private Long id;
    
    @Column(name = "yeg_gru_nombre", nullable = false, unique = true, length = 100)
    private String nombre;
    
    @Column(name = "yeg_gru_icono", length = 50)
    private String icono;
    
    @Column(name = "yeg_gru_activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "yeg_gru_fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}

