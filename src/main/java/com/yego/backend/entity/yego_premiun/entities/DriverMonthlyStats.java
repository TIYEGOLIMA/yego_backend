package com.yego.backend.entity.yego_premiun.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "driver_monthly_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverMonthlyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private String driverId;

    @Column(name = "park_id", nullable = false)
    private String parkId;

    @Column(name = "month")
    private Integer month;

    @Column(name = "year")
    private Integer year;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "count_orders_completed")
    private Integer countOrdersCompleted;

    @Column(name = "count_orders_all")
    private Integer countOrdersAll;

    @Column(name = "count_orders_accepted")
    private Integer countOrdersAccepted;

    @Column(name = "count_orders_cancelled_by_client")
    private Integer countOrdersCancelledByClient;

    @Column(name = "count_orders_cancelled_by_driver")
    private Integer countOrdersCancelledByDriver;

    @Column(name = "count_orders_platform")
    private Integer countOrdersPlatform;

    @Transient
    private Integer countActiveDrivers;

    @Transient
    private Integer countDrivers;

    @Transient
    private BigDecimal acceptanceRate;

    @Transient
    private BigDecimal completionRate;

    @Transient
    private BigDecimal sumDistance;

    @Transient
    private Integer sumOrdersCompleted;

    @Column(name = "sum_price_cash", precision = 10, scale = 2)
    private BigDecimal sumPriceCash;

    @Column(name = "sum_price_cashless", precision = 10, scale = 2)
    private BigDecimal sumPriceCashless;

    @Column(name = "sum_price_other_gas", precision = 10, scale = 2)
    private BigDecimal sumPriceOtherGas;

    @Column(name = "sum_price_park_commission", precision = 10, scale = 2)
    private BigDecimal sumPriceParkCommission;

    @Column(name = "sum_price_platform_commission", precision = 10, scale = 2)
    private BigDecimal sumPricePlatformCommission;

    @Column(name = "sum_work_time_seconds")
    private Long sumWorkTimeSeconds;

    @Transient
    private BigDecimal tripsPerHour;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "category_synced")
    private Boolean categorySynced;

    @Transient
    private LocalDateTime categorySyncedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now(ZoneId.of("America/Lima")).toLocalDateTime();
        }
        if (categorySynced == null) {
            categorySynced = Boolean.FALSE;
        }
    }
}

