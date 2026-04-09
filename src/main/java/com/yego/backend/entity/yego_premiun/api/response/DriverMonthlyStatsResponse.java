package com.yego.backend.entity.yego_premiun.api.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class DriverMonthlyStatsResponse {

    Long id;
    String driverId;
    /** ID del parque/flota (mismo que en API partners); para filtros y listas. */
    String parkId;
    String parkName;
    Integer month;
    Integer year;
    String category;
    Integer countOrdersCompleted;
    Integer countOrdersAll;
    Integer countOrdersAccepted;
    Integer countOrdersCancelledByClient;
    Integer countOrdersCancelledByDriver;
    Integer countOrdersPlatform;
    BigDecimal sumPriceCash;
    BigDecimal sumPriceCashless;
    BigDecimal sumPriceOtherGas;
    BigDecimal sumPriceParkCommission;
    BigDecimal sumPricePlatformCommission;
    Long sumWorkTimeSeconds;
    LocalDateTime createdAt;
    String fullName;
    String phone;
    String licenseNumber;
    Boolean categorySynced;
    LocalDateTime categorySyncedAt;
    String categoryDetail;
    LocalDate hireDate;
}

