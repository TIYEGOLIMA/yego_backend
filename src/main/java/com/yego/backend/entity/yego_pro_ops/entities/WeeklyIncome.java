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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_weekly_income")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", nullable = false, length = 255)
    private String driverId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "bonificacion", precision = 12, scale = 2)
    private BigDecimal bonificacion;

    @Column(name = "cash_collected", precision = 12, scale = 2)
    private BigDecimal cashCollected;

    @Column(name = "non_cash_payment", precision = 12, scale = 2)
    private BigDecimal nonCashPayment;

    @Column(name = "corporate", precision = 12, scale = 2)
    private BigDecimal corporate;

    @Column(name = "tips", precision = 12, scale = 2)
    private BigDecimal tips;

    @Column(name = "promotion_compensation", precision = 12, scale = 2)
    private BigDecimal promotionCompensation;

    @Column(name = "platform_fees", precision = 12, scale = 2)
    private BigDecimal platformFees;

    @Column(name = "total", precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "price_yango_pro", precision = 12, scale = 2)
    private BigDecimal priceYangoPro;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
