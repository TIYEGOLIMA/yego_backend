package com.yego.backend.entity.yego_principal.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_modulos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Module {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yeg_sis_ext_id")
    private Long id;
    
    @Column(name = "yeg_sis_ext_nombre", nullable = false, length = 100)
    private String nombre;
    
    @Column(name = "yeg_sis_ext_descripcion", length = 500)
    private String descripcion;
    
    @Column(name = "yeg_sis_ext_url", nullable = false, length = 500)
    private String url;
    
    @Column(name = "yeg_sis_ext_estado", length = 50, nullable = false)
    private String estado;
    
    @Column(name = "yeg_sis_ext_ultimo_check")
    private LocalDateTime ultimoCheck;
    
    @Column(name = "yeg_sis_ext_activo", nullable = false)
    private Boolean activo = true;
    
    @CreationTimestamp
    @Column(name = "yeg_sis_ext_created_at", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    @UpdateTimestamp
    @Column(name = "yeg_sis_ext_updated_at", nullable = false)
    private LocalDateTime fechaActualizacion;
    
}
