package com.yego.backend.entity.yego_principal.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad para sistemas externos del sistema YEGO Principal
 */
@Entity
@Table(name = "yego_sistema_externo_dev")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SistemaExterno {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "yeg_sis_ext_id")
    private Long yegoSisExtId;
    
    @Column(name = "yeg_sis_ext_nombre", nullable = false, length = 100)
    private String yegoSisExtNombre;
    
    @Column(name = "yeg_sis_ext_descripcion", length = 255)
    private String yegoSisExtDescripcion;
    
    @Column(name = "yeg_sis_ext_url", nullable = false, length = 500)
    private String yegoSisExtUrl; // URL completa, ej: "http://localhost:5174/garantizado"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "yeg_sis_ext_estado", nullable = false, length = 20)
    private EstadoSistema yegoSisExtEstado;
    
    
    @Column(name = "yeg_sis_ext_ultimo_check")
    private LocalDateTime yegoSisExtUltimoCheck;
    
    @Column(name = "yeg_sis_ext_activo", nullable = false)
    private Boolean yegoSisExtActivo;
    
    @CreationTimestamp
    @Column(name = "yeg_sis_ext_created_at", nullable = false, updatable = false)
    private LocalDateTime yegoSisExtCreatedAt;
    
    @UpdateTimestamp
    @Column(name = "yeg_sis_ext_updated_at", nullable = false)
    private LocalDateTime yegoSisExtUpdatedAt;
    
    /**
     * Estados posibles para un sistema externo
     */
    public enum EstadoSistema {
        ACTIVO,
        INACTIVO
    }
}
