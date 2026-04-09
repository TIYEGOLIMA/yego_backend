package com.yego.backend.service.yego_premiun.impl;

import com.yego.backend.entity.yego_garantizado.entities.Driver;
import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;
import com.yego.backend.entity.yego_premiun.entities.DriverActiveList;
import com.yego.backend.repository.yego_garantizado.DriverRepository;
import com.yego.backend.repository.yego_premiun.DriverActiveListRepository;
import com.yego.backend.service.yego_premiun.DriverMonthlyStatsService;
import com.yego.backend.service.yego_premiun.FlotaLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverMonthlyStatsServiceImpl implements DriverMonthlyStatsService {

    private final DriverActiveListRepository driverActiveListRepository;
    private final DriverRepository driverRepository;
    private final FlotaLookupService flotaLookupService;
    private final JdbcTemplate jdbcTemplate;

    private static final String PROCESS_SQL = """
            WITH trip_agg AS (
                SELECT
                    t.conductor_id,
                    (MODE() WITHIN GROUP (ORDER BY t.park_id)) AS park_id,
                    COUNT(*) FILTER (WHERE t.condicion IN ('Completado','Выполнен')) AS trips,
                    COUNT(*) AS count_all,
                    COUNT(*) FILTER (WHERE t.motivo_cancelacion IS NULL
                        OR t.motivo_cancelacion NOT IN ('El conducto rechazó la solicitud de viaje','El conductor no aceptó la solicitud de viaje')
                    ) AS count_accepted,
                    COUNT(*) FILTER (WHERE t.condicion IN ('Cancelado','Отменён')
                        AND t.motivo_cancelacion = 'Viaje cancelado por el cliente'
                    ) AS cancel_client,
                    COUNT(*) FILTER (WHERE t.condicion IN ('Cancelado','Отменён')
                        AND t.motivo_cancelacion IN ('El conducto rechazó la solicitud de viaje','El conductor no aceptó la solicitud de viaje')
                    ) AS cancel_driver,
                    ROUND(COALESCE(SUM(CASE WHEN t.condicion IN ('Completado','Выполнен') THEN t.efectivo ELSE 0 END)/100.0,0),2) AS cash,
                    ROUND(COALESCE(SUM(CASE WHEN t.condicion IN ('Completado','Выполнен') THEN COALESCE(t.tarjeta,0)+COALESCE(t.pago_corporativo,0) ELSE 0 END)/100.0,0),2) AS cashless,
                    ROUND(COALESCE(SUM(CASE WHEN t.condicion IN ('Completado','Выполнен') THEN COALESCE(t.otros_pagos,0) ELSE 0 END)/100.0,0),2) AS other_gas,
                    ROUND(COALESCE(SUM(CASE WHEN t.condicion IN ('Completado','Выполнен') THEN COALESCE(t.comision_empresa_asociada,0) ELSE 0 END)/100.0,0),2) AS park_comm,
                    ROUND(COALESCE(SUM(CASE WHEN t.condicion IN ('Completado','Выполнен') THEN COALESCE(t.comision_servicio,0) ELSE 0 END)/100.0,0),2) AS plat_comm,
                    COALESCE(SUM(CASE WHEN t.condicion IN ('Completado','Выполнен')
                        AND t.fecha_finalizacion IS NOT NULL AND t.fecha_inicio_viaje IS NOT NULL
                        THEN GREATEST(EXTRACT(EPOCH FROM (t.fecha_finalizacion - t.fecha_inicio_viaje))::bigint, 0) ELSE 0 END
                    ), 0) AS work_secs
                FROM %s t
                WHERE t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
                  AND t.conductor_id IS NOT NULL AND t.conductor_id != ''
                GROUP BY t.conductor_id
            ),
            classified AS (
                SELECT
                    a.conductor_id AS driver_id,
                    COALESCE(NULLIF(a.park_id,''), d.park_id) AS park_id,
                    a.trips,
                    ?::int AS month,
                    ?::int AS year,
                    ((? * 12 + ?) - (EXTRACT(YEAR FROM d.hire_date)::int * 12 + EXTRACT(MONTH FROM d.hire_date)::int)) + 1 AS months_since,
                    a.count_all, a.count_accepted, a.cancel_client, a.cancel_driver,
                    a.cash, a.cashless, a.other_gas, a.park_comm, a.plat_comm, a.work_secs
                FROM trip_agg a
                JOIN drivers d ON d.driver_id = a.conductor_id
                WHERE d.hire_date IS NOT NULL
                  AND COALESCE(NULLIF(a.park_id,''), d.park_id) IS NOT NULL
                  AND COALESCE(NULLIF(a.park_id,''), d.park_id) != ''
            )
            INSERT INTO driver_active_list (
                driver_id, park_id, trips, month, year, category,
                count_orders_completed, count_orders_all, count_orders_accepted,
                count_orders_cancelled_by_client, count_orders_cancelled_by_driver, count_orders_platform,
                sum_price_cash, sum_price_cashless, sum_price_other_gas,
                sum_price_park_commission, sum_price_platform_commission,
                sum_work_time_seconds, created_at, updated_at
            )
            SELECT
                c.driver_id, c.park_id, c.trips, c.month, c.year,
                CASE
                    WHEN c.trips >= 400 THEN 'premiun oro'
                    WHEN c.trips >= (CASE LEAST(c.months_since, 3) WHEN 1 THEN 50 WHEN 2 THEN 100 ELSE 200 END) THEN 'premiun plata'
                END,
                c.trips, c.count_all, c.count_accepted,
                c.cancel_client, c.cancel_driver, c.count_all,
                c.cash, c.cashless, c.other_gas,
                c.park_comm, c.plat_comm,
                c.work_secs, NOW(), NOW()
            FROM classified c
            WHERE c.months_since > 0
              AND (
                  c.trips >= 400
                  OR c.trips >= (CASE LEAST(c.months_since, 3) WHEN 1 THEN 50 WHEN 2 THEN 100 ELSE 200 END)
              )
            """;

    @Override
    @Transactional
    public List<DriverMonthlyStatsResponse> procesarYListarActivos(Integer month, Integer year) {
        String tableName = "trips_" + year;

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, tableName);

        if (!Boolean.TRUE.equals(exists)) {
            log.warn("[DriverMonthlyStatsService] Tabla {} no existe", tableName);
            return Collections.emptyList();
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate rangeStart = ym.atDay(1);
        LocalDate rangeEnd = ym.plusMonths(1).atDay(1);

        log.info("[DriverMonthlyStatsService] Procesando {} rango [{}, {})", tableName, rangeStart, rangeEnd);
        long t0 = System.currentTimeMillis();

        jdbcTemplate.update("DELETE FROM driver_active_list WHERE month = ? AND year = ?", month, year);

        String sql = String.format(PROCESS_SQL, tableName);

        int inserted = jdbcTemplate.update(sql,
                rangeStart, rangeEnd,
                month, year,
                year, month
        );

        long elapsed = System.currentTimeMillis() - t0;
        log.info("[DriverMonthlyStatsService] {} conductores premiun insertados en {}ms", inserted, elapsed);

        if (inserted == 0) {
            return Collections.emptyList();
        }

        List<DriverActiveList> activos = driverActiveListRepository.findAll();
        return construirRespuestas(activos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverMonthlyStatsResponse> listarActivos() {
        List<DriverActiveList> activos = driverActiveListRepository.findAll();
        if (activos.isEmpty()) {
            return Collections.emptyList();
        }
        return construirRespuestas(activos);
    }

    private List<DriverMonthlyStatsResponse> construirRespuestas(List<DriverActiveList> activos) {
        Set<String> driverIds = activos.stream()
                .map(DriverActiveList::getDriverId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        Map<String, Driver> driverMap = driverIds.isEmpty()
                ? Collections.emptyMap()
                : driverRepository.findAllById(driverIds).stream()
                .collect(Collectors.toMap(Driver::getDriverId, Function.identity()));

        return activos.stream()
                .map(activo -> buildResponse(activo, driverMap.get(activo.getDriverId())))
                .collect(Collectors.toList());
    }

    private DriverMonthlyStatsResponse buildResponse(DriverActiveList activo, Driver driver) {
        String parkName = flotaLookupService.obtenerNombreFlota(activo.getParkId());

        return DriverMonthlyStatsResponse.builder()
                .id(activo.getId())
                .driverId(activo.getDriverId())
                .parkId(activo.getParkId())
                .parkName(parkName)
                .month(activo.getMonth())
                .year(activo.getYear())
                .category(activo.getCategory())
                .countOrdersCompleted(activo.getCountOrdersCompleted())
                .countOrdersAll(activo.getCountOrdersAll())
                .countOrdersAccepted(activo.getCountOrdersAccepted())
                .countOrdersCancelledByClient(activo.getCountOrdersCancelledByClient())
                .countOrdersCancelledByDriver(activo.getCountOrdersCancelledByDriver())
                .countOrdersPlatform(activo.getCountOrdersPlatform())
                .sumPriceCash(activo.getSumPriceCash())
                .sumPriceCashless(activo.getSumPriceCashless())
                .sumPriceOtherGas(activo.getSumPriceOtherGas())
                .sumPriceParkCommission(activo.getSumPriceParkCommission())
                .sumPricePlatformCommission(activo.getSumPricePlatformCommission())
                .sumWorkTimeSeconds(activo.getSumWorkTimeSeconds())
                .createdAt(activo.getCreatedAt())
                .fullName(driver != null ? driver.getFullName() : null)
                .phone(driver != null ? driver.getPhone() : null)
                .licenseNumber(driver != null ? driver.getLicenseNumber() : null)
                .categorySynced(Boolean.TRUE)
                .categorySyncedAt(activo.getUpdatedAt())
                .categoryDetail(generarDetalle(activo, driver))
                .hireDate(driver != null ? driver.getHireDate() : null)
                .build();
    }

    private String generarDetalle(DriverActiveList activo, Driver driver) {
        if (activo.getYear() == null || activo.getMonth() == null) return "Sin datos";
        LocalDate hireDate = driver != null ? driver.getHireDate() : null;
        if (hireDate == null) return "Sin fecha de vinculación";

        YearMonth periodo = YearMonth.of(activo.getYear(), activo.getMonth());
        YearMonth hireMonth = YearMonth.from(hireDate);
        if (hireMonth.isAfter(periodo)) return "Aún no cumple un mes desde la vinculación";

        long meses = hireMonth.until(periodo, ChronoUnit.MONTHS) + 1;
        int trips = Optional.ofNullable(activo.getTrips()).orElse(0);

        if ("premiun oro".equalsIgnoreCase(activo.getCategory())) {
            return String.format("Mes %d desde vinculación, %d viajes ≥ 400 → premiun oro", meses, trips);
        }

        long mesEvaluado = Math.min(meses, 3);
        int umbral = mesEvaluado == 1 ? 50 : mesEvaluado == 2 ? 100 : 200;
        return String.format("Mes %d desde vinculación, %d viajes (mínimo %d) → %s",
                meses, trips, umbral, Optional.ofNullable(activo.getCategory()).orElse("premiun plata"));
    }
}
