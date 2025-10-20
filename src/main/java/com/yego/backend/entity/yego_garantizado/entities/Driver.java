package com.yego.backend.entity.yego_garantizado.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Entidad de solo lectura para la tabla drivers
 * No se usa para guardar, solo para consultas
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Entity
@Table(name = "drivers")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @Column(name = "driver_id")
    private String driverId;

    @Column(name = "park_id")
    private String parkId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "license_number")
    private String licenseNumber;
}

