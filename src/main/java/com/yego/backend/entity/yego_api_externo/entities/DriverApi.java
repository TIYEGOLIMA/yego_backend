package com.yego.backend.entity.yego_api_externo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad completa para la tabla drivers
 * Contiene todas las columnas de la tabla drivers de PostgreSQL
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
public class DriverApi {

    @Id
    @Column(name = "driver_id")
    private String driverId;

    @Column(name = "park_id")
    private String parkId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "work_status")
    private String workStatus;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "fire_date")
    private LocalDate fireDate;

    @Column(name = "is_selfemployed")
    private Boolean isSelfemployed;

    @Column(name = "car_id")
    private String carId;

    @Column(name = "car_brand")
    private String carBrand;

    @Column(name = "car_model")
    private String carModel;

    @Column(name = "car_color")
    private String carColor;

    @Column(name = "car_number")
    private String carNumber;

    @Column(name = "car_callsign")
    private String carCallsign;

    @Column(name = "car_normalized_number")
    private String carNormalizedNumber;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_country")
    private String licenseCountry;

    @Column(name = "license_expiration_date")
    private LocalDate licenseExpirationDate;

    @Column(name = "license_issue_date")
    private LocalDate licenseIssueDate;

    @Column(name = "license_normalized_number")
    private String licenseNormalizedNumber;

    @Column(name = "account_balance")
    private BigDecimal accountBalance;

    @Column(name = "account_balance_limit")
    private BigDecimal accountBalanceLimit;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "current_status")
    private String currentStatus;

    @Column(name = "status_updated_at")
    private LocalDate statusUpdatedAt;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "id")
    private Long id;

    @Column(name = "active")
    private Boolean active;
}

