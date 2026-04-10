package com.yego.backend.service.yego_premiun.impl;

import com.yego.backend.entity.yego_premiun.api.response.DailyTripsPointResponse;
import com.yego.backend.entity.yego_premiun.api.response.DriverTripsMonthResponse;
import com.yego.backend.entity.yego_premiun.api.response.DriverTripsYearResponse;
import com.yego.backend.entity.yego_premiun.api.response.MonthlyTripsAggregateResponse;
import com.yego.backend.entity.yego_premiun.api.response.TripCompletedItemResponse;
import com.yego.backend.service.yego_premiun.DriverTripsMonthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverTripsMonthServiceImpl implements DriverTripsMonthService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Divisor aplicado a {@code SUM(precio_yango_pro)}. En BD, {@code precio_yango_pro} suele ser {@code numeric} ya en soles → 1.
     * Usar 100 solo si la columna almacena centavos (legacy).
     */
    @Value("${yego.premium.yango-pro-divisor:1}")
    private BigDecimal yangoProDivisor;

    private static final String TRIPS_SQL = """
            SELECT t.fecha_inicio_viaje, t.fecha_finalizacion, t.condicion, t.park_id
            FROM %s t
            WHERE t.conductor_id = ?
              AND t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
              AND t.condicion IN ('Completado','Выполнен')
            ORDER BY t.fecha_inicio_viaje ASC
            """;

    private static final String DAILY_WITH_YANGO_SQL = """
            SELECT
              (t.fecha_inicio_viaje::date) AS d,
              COUNT(*)::int AS trips,
              ROUND(COALESCE(SUM(COALESCE(t.precio_yango_pro, 0)), 0) / ?, 2) AS yango_soles
            FROM %s t
            WHERE t.conductor_id = ?
              AND t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
              AND t.condicion IN ('Completado','Выполнен')
            GROUP BY (t.fecha_inicio_viaje::date)
            ORDER BY d
            """;

    private static final String DAILY_NO_YANGO_SQL = """
            SELECT
              (t.fecha_inicio_viaje::date) AS d,
              COUNT(*)::int AS trips,
              0::numeric AS yango_soles
            FROM %s t
            WHERE t.conductor_id = ?
              AND t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
              AND t.condicion IN ('Completado','Выполнен')
            GROUP BY (t.fecha_inicio_viaje::date)
            ORDER BY d
            """;

    private static final String YEARLY_WITH_YANGO_SQL = """
            SELECT
              EXTRACT(MONTH FROM t.fecha_inicio_viaje)::int AS m,
              COUNT(*)::int AS trips,
              ROUND(COALESCE(SUM(COALESCE(t.precio_yango_pro, 0)), 0) / ?, 2) AS yango_soles
            FROM %s t
            WHERE t.conductor_id = ?
              AND t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
              AND t.condicion IN ('Completado','Выполнен')
            GROUP BY EXTRACT(MONTH FROM t.fecha_inicio_viaje)
            ORDER BY m
            """;

    /** Suma anual de precio_yango_pro (misma fórmula que SUM manual en SQL / divisor). */
    private static final String YEAR_TOTAL_YANGO_SQL = """
            SELECT ROUND(COALESCE(SUM(COALESCE(t.precio_yango_pro, 0)), 0) / ?, 2)
            FROM %s t
            WHERE t.conductor_id = ?
              AND t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
              AND t.condicion IN ('Completado','Выполнен')
            """;

    private static final String YEARLY_NO_YANGO_SQL = """
            SELECT
              EXTRACT(MONTH FROM t.fecha_inicio_viaje)::int AS m,
              COUNT(*)::int AS trips,
              0::numeric AS yango_soles
            FROM %s t
            WHERE t.conductor_id = ?
              AND t.fecha_inicio_viaje >= ? AND t.fecha_inicio_viaje < ?
              AND t.condicion IN ('Completado','Выполнен')
            GROUP BY EXTRACT(MONTH FROM t.fecha_inicio_viaje)
            ORDER BY m
            """;

    @Override
    @Transactional(readOnly = true)
    public DriverTripsMonthResponse listCompletedTripsForMonth(String driverId, int month, int year) {
        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException("driverId es obligatorio");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month debe estar entre 1 y 12");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year fuera de rango");
        }

        String trimmedId = driverId.trim();
        String tableName = "trips_" + year;

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class,
                tableName);

        YearMonth ym = YearMonth.of(year, month);

        if (!Boolean.TRUE.equals(exists)) {
            log.debug("[DriverTripsMonth] Tabla {} no existe", tableName);
            return emptyResponse(trimmedId, ym);
        }

        LocalDate rangeStart = ym.atDay(1);
        LocalDate rangeEnd = ym.plusMonths(1).atDay(1);

        boolean hasYangoCol = hasColumn(tableName, "precio_yango_pro");
        if (!hasYangoCol) {
            log.debug("[DriverTripsMonth] Tabla {} sin columna precio_yango_pro; serie Yango Pro en 0", tableName);
        }

        String dailySql = String.format(hasYangoCol ? DAILY_WITH_YANGO_SQL : DAILY_NO_YANGO_SQL, tableName);
        List<DailyTripsPointResponse> dailySparse = hasYangoCol
                ? jdbcTemplate.query(
                        dailySql,
                        (rs, rowNum) -> mapDailyRow(rs),
                        yangoProDivisor,
                        trimmedId,
                        rangeStart,
                        rangeEnd)
                : jdbcTemplate.query(
                        dailySql,
                        (rs, rowNum) -> mapDailyRow(rs),
                        trimmedId,
                        rangeStart,
                        rangeEnd);

        Map<LocalDate, DailyTripsPointResponse> byDay = new HashMap<>();
        for (DailyTripsPointResponse p : dailySparse) {
            byDay.put(p.getDate(), p);
        }

        List<DailyTripsPointResponse> dailySeries = fillMonth(ym, byDay);

        BigDecimal totalYango = dailySeries.stream()
                .map(DailyTripsPointResponse::getPrecioYangoProSoles)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String sql = String.format(TRIPS_SQL, tableName);
        List<TripCompletedItemResponse> trips = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs),
                trimmedId,
                rangeStart,
                rangeEnd
        );

        int completed = trips.size();

        return DriverTripsMonthResponse.builder()
                .driverId(trimmedId)
                .month(month)
                .year(year)
                .completedTripsCount(completed)
                .totalPrecioYangoProSoles(totalYango.setScale(2, RoundingMode.HALF_UP))
                .dailySeries(dailySeries)
                .trips(trips)
                .build();
    }

    private static DriverTripsMonthResponse emptyResponse(String driverId, YearMonth ym) {
        List<DailyTripsPointResponse> flat = new ArrayList<>(ym.lengthOfMonth());
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            flat.add(DailyTripsPointResponse.builder()
                    .date(ym.atDay(d))
                    .tripsCount(0)
                    .precioYangoProSoles(BigDecimal.ZERO.setScale(2))
                    .build());
        }
        return DriverTripsMonthResponse.builder()
                .driverId(driverId)
                .month(ym.getMonthValue())
                .year(ym.getYear())
                .completedTripsCount(0)
                .totalPrecioYangoProSoles(BigDecimal.ZERO.setScale(2))
                .dailySeries(flat)
                .trips(Collections.emptyList())
                .build();
    }

    private boolean hasColumn(String tableName, String columnName) {
        Boolean b = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                          SELECT 1 FROM information_schema.columns
                          WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                        )
                        """,
                Boolean.class,
                tableName,
                columnName);
        return Boolean.TRUE.equals(b);
    }

    private static List<DailyTripsPointResponse> fillMonth(YearMonth ym, Map<LocalDate, DailyTripsPointResponse> byDay) {
        List<DailyTripsPointResponse> out = new ArrayList<>(ym.lengthOfMonth());
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            DailyTripsPointResponse p = byDay.get(date);
            if (p != null) {
                out.add(p);
            } else {
                out.add(DailyTripsPointResponse.builder()
                        .date(date)
                        .tripsCount(0)
                        .precioYangoProSoles(BigDecimal.ZERO.setScale(2))
                        .build());
            }
        }
        return out;
    }

    private static DailyTripsPointResponse mapDailyRow(ResultSet rs) throws SQLException {
        Date d = rs.getDate("d");
        LocalDate date = d != null ? d.toLocalDate() : null;
        int trips = rs.getInt("trips");
        BigDecimal yango = rs.getBigDecimal("yango_soles");
        if (yango == null) {
            yango = BigDecimal.ZERO;
        }
        return DailyTripsPointResponse.builder()
                .date(date)
                .tripsCount(trips)
                .precioYangoProSoles(yango.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverTripsYearResponse listCompletedTripsForYear(String driverId, int year) {
        if (driverId == null || driverId.isBlank()) {
            throw new IllegalArgumentException("driverId es obligatorio");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year fuera de rango");
        }

        String trimmedId = driverId.trim();
        String tableName = "trips_" + year;

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class,
                tableName);

        if (!Boolean.TRUE.equals(exists)) {
            log.debug("[DriverTripsMonth] Tabla {} no existe (año)", tableName);
            return emptyYearResponse(trimmedId, year);
        }

        LocalDate rangeStart = LocalDate.of(year, 1, 1);
        LocalDate rangeEnd = LocalDate.of(year + 1, 1, 1);

        boolean hasYangoCol = hasColumn(tableName, "precio_yango_pro");
        if (!hasYangoCol) {
            log.debug("[DriverTripsMonth] Tabla {} sin precio_yango_pro (año)", tableName);
        }

        String yearlySql = String.format(hasYangoCol ? YEARLY_WITH_YANGO_SQL : YEARLY_NO_YANGO_SQL, tableName);
        List<MonthlyTripsAggregateResponse> sparse = hasYangoCol
                ? jdbcTemplate.query(
                        yearlySql,
                        DriverTripsMonthServiceImpl::mapMonthlyAggregateRow,
                        yangoProDivisor,
                        trimmedId,
                        rangeStart,
                        rangeEnd)
                : jdbcTemplate.query(
                        yearlySql,
                        DriverTripsMonthServiceImpl::mapMonthlyAggregateRow,
                        trimmedId,
                        rangeStart,
                        rangeEnd);

        sparse.sort(Comparator.comparing(MonthlyTripsAggregateResponse::getMonth));

        int totalTrips = sparse.stream()
                .mapToInt(p -> p.getTripsCount() != null ? p.getTripsCount() : 0)
                .sum();

        BigDecimal totalYango;
        if (hasYangoCol) {
            String totalSql = String.format(YEAR_TOTAL_YANGO_SQL, tableName);
            BigDecimal t = jdbcTemplate.queryForObject(
                    totalSql,
                    BigDecimal.class,
                    yangoProDivisor,
                    trimmedId,
                    rangeStart,
                    rangeEnd);
            totalYango = t != null ? t : BigDecimal.ZERO;
        } else {
            totalYango = BigDecimal.ZERO;
        }

        return DriverTripsYearResponse.builder()
                .driverId(trimmedId)
                .year(year)
                .totalCompletedTrips(totalTrips)
                .totalPrecioYangoProSoles(totalYango.setScale(2, RoundingMode.HALF_UP))
                .monthlySeries(sparse)
                .build();
    }

    private static DriverTripsYearResponse emptyYearResponse(String driverId, int year) {
        return DriverTripsYearResponse.builder()
                .driverId(driverId)
                .year(year)
                .totalCompletedTrips(0)
                .totalPrecioYangoProSoles(BigDecimal.ZERO.setScale(2))
                .monthlySeries(Collections.emptyList())
                .build();
    }

    private static MonthlyTripsAggregateResponse mapMonthlyAggregateRow(ResultSet rs, int rowNum) throws SQLException {
        int m = rs.getInt("m");
        int trips = rs.getInt("trips");
        BigDecimal yango = rs.getBigDecimal("yango_soles");
        if (yango == null) {
            yango = BigDecimal.ZERO;
        }
        return MonthlyTripsAggregateResponse.builder()
                .month(m)
                .tripsCount(trips)
                .precioYangoProSoles(yango.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private static TripCompletedItemResponse mapRow(ResultSet rs) throws SQLException {
        Timestamp tsIni = rs.getTimestamp("fecha_inicio_viaje");
        Timestamp tsFin = rs.getTimestamp("fecha_finalizacion");
        return TripCompletedItemResponse.builder()
                .fechaInicioViaje(tsIni != null ? tsIni.toLocalDateTime() : null)
                .fechaFinalizacion(tsFin != null ? tsFin.toLocalDateTime() : null)
                .condicion(rs.getString("condicion"))
                .parkId(rs.getString("park_id"))
                .build();
    }
}
