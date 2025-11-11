package com.yego.backend.entity.yego_premiun.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "driver_active_list")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverActiveList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "driver_id", nullable = false, unique = true)
    private String driverId;

    @Column(name = "park_id", nullable = false)
    private String parkId;

    @Column(name = "trips")
    private Integer trips;

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

    @Column(name = "count_active_drivers")
    private Integer countActiveDrivers;

    @Column(name = "count_drivers")
    private Integer countDrivers;

    @Column(name = "acceptance_rate", precision = 10, scale = 6)
    private BigDecimal acceptanceRate;

    @Column(name = "completion_rate", precision = 10, scale = 6)
    private BigDecimal completionRate;

    @Column(name = "sum_distance", precision = 15, scale = 6)
    private BigDecimal sumDistance;

    @Column(name = "sum_orders_completed")
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

    @Column(name = "trips_per_hour", precision = 10, scale = 6)
    private BigDecimal tripsPerHour;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime ahoraLima = ZonedDateTime.now(ZoneId.of("America/Lima")).toLocalDateTime();
        if (createdAt == null) {
            createdAt = ahoraLima;
        }
        updatedAt = ahoraLima;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now(ZoneId.of("America/Lima")).toLocalDateTime();
    }
}


