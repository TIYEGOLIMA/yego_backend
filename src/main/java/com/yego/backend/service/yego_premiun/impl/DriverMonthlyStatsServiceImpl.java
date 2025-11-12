package com.yego.backend.service.yego_premiun.impl;

import com.yego.backend.entity.yego_garantizado.entities.Driver;
import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;
import com.yego.backend.entity.yego_premiun.entities.DriverActiveList;
import com.yego.backend.entity.yego_premiun.entities.DriverMonthlyStats;
import com.yego.backend.repository.yego_garantizado.DriverRepository;
import com.yego.backend.repository.yego_premiun.DriverActiveListRepository;
import com.yego.backend.repository.yego_premiun.DriverMonthlyStatsRepository;
import com.yego.backend.service.yego_premiun.DriverMonthlyStatsService;
import com.yego.backend.service.yego_premiun.FlotaLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverMonthlyStatsServiceImpl implements DriverMonthlyStatsService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final DriverMonthlyStatsRepository driverMonthlyStatsRepository;
    private final DriverActiveListRepository driverActiveListRepository;
    private final DriverRepository driverRepository;
    private final FlotaLookupService flotaLookupService;

    @Override
    @Transactional
    public List<DriverMonthlyStatsResponse> procesarYListarActivos() {
        YearMonth mesEvaluacion = obtenerMesEvaluacion();
        log.info("📊 [DriverMonthlyStatsService] Iniciando proceso de sincronización de driver_active_list para {}-{}", mesEvaluacion.getYear(), mesEvaluacion.getMonthValue());

        List<DriverMonthlyStats> stats = driverMonthlyStatsRepository.findAllByYearAndMonth(mesEvaluacion.getYear(), mesEvaluacion.getMonthValue());
        if (stats.isEmpty()) {
            log.info("ℹ️ [DriverMonthlyStatsService] No se encontraron registros en driver_monthly_stats para procesar");
            driverActiveListRepository.deleteAll();
            log.info("🧹 [DriverMonthlyStatsService] driver_active_list fue limpiada por falta de datos");
            return Collections.emptyList();
        }

        List<DriverActiveList> existentes = driverActiveListRepository.findAll();

        Map<String, DriverActiveList> activosPorDriver = existentes.stream()
                .filter(activo -> esDriverIdValido(activo.getDriverId()))
                .collect(Collectors.toMap(DriverActiveList::getDriverId, Function.identity(), (actual, ignorado) -> actual));

        Set<Long> idsParaEliminar = existentes.stream()
                .filter(activo -> !esDriverIdValido(activo.getDriverId()))
                .map(DriverActiveList::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> driverIds = stats.stream()
                .map(DriverMonthlyStats::getDriverId)
                .filter(this::esDriverIdValido)
                .collect(Collectors.toSet());

        Map<String, Driver> driverMap = driverIds.isEmpty()
                ? Collections.emptyMap()
                : driverRepository.findAllById(driverIds).stream()
                .collect(Collectors.toMap(Driver::getDriverId, Function.identity()));

        List<DriverActiveList> activosParaGuardar = new ArrayList<>();
        List<DriverMonthlyStats> statsParaActualizar = new ArrayList<>();

        for (DriverMonthlyStats stat : stats) {
            String driverId = stat.getDriverId();
            if (!esDriverIdValido(driverId)) {
                log.warn("⚠️ [DriverMonthlyStatsService] driver_id inválido para registro id={}", stat.getId());
                marcarSincronizacion(stat, false, statsParaActualizar);
                continue;
            }

            Driver driver = driverMap.get(driverId);
            LocalDate hireDate = driver != null ? driver.getHireDate() : null;
            if (hireDate == null) {
                log.debug("ℹ️ [DriverMonthlyStatsService] Se omite driver {} por no tener hire_date", driverId);
                eliminarActivoSiExiste(activosPorDriver, driverId, idsParaEliminar);
                marcarSincronizacion(stat, false, statsParaActualizar);
                continue;
            }

            long mesesDesdeVinculacion = calcularMesesDesdeVinculacion(hireDate, mesEvaluacion);
            if (mesesDesdeVinculacion <= 0) {
                log.debug("ℹ️ [DriverMonthlyStatsService] Se omite driver {} porque aún no cumple un mes desde su vinculación", driverId);
                eliminarActivoSiExiste(activosPorDriver, driverId, idsParaEliminar);
                marcarSincronizacion(stat, false, statsParaActualizar);
                continue;
            }

            int trips = obtenerTrips(stat);
            Optional<String> categoriaOpt = determinarCategoria(trips, mesesDesdeVinculacion);
            if (categoriaOpt.isEmpty()) {
                eliminarActivoSiExiste(activosPorDriver, driverId, idsParaEliminar);
                marcarSincronizacion(stat, false, statsParaActualizar);
                continue;
            }

            String parkId = resolverParkId(stat, driver);
            if (!esDriverIdValido(parkId)) {
                log.warn("⚠️ [DriverMonthlyStatsService] No se pudo determinar park_id para driver {}", driverId);
                eliminarActivoSiExiste(activosPorDriver, driverId, idsParaEliminar);
                marcarSincronizacion(stat, false, statsParaActualizar);
                continue;
            }

            DriverActiveList activo = activosPorDriver.remove(driverId);
            if (activo == null) {
                activo = DriverActiveList.builder()
                        .driverId(driverId)
                        .build();
            }

            actualizarActivo(activo, stat, categoriaOpt.get(), trips, parkId);
            activosParaGuardar.add(activo);

            marcarSincronizacion(stat, true, statsParaActualizar);
        }

        activosPorDriver.values().stream()
                .map(DriverActiveList::getId)
                .filter(Objects::nonNull)
                .forEach(idsParaEliminar::add);

        if (!idsParaEliminar.isEmpty()) {
            driverActiveListRepository.deleteAllById(idsParaEliminar);
            log.info("🧹 [DriverMonthlyStatsService] Eliminados {} registros de driver_active_list", idsParaEliminar.size());
        }

        if (!activosParaGuardar.isEmpty()) {
            driverActiveListRepository.saveAll(activosParaGuardar);
            log.info("💾 [DriverMonthlyStatsService] Guardados/actualizados {} registros en driver_active_list", activosParaGuardar.size());
        }

        if (!statsParaActualizar.isEmpty()) {
            driverMonthlyStatsRepository.saveAll(statsParaActualizar);
            log.info("🔄 [DriverMonthlyStatsService] Columnas de sincronización actualizadas para {} registros en driver_monthly_stats", statsParaActualizar.size());
        }

        List<DriverActiveList> activos = driverActiveListRepository.findAll();
        if (activos.isEmpty()) {
            log.info("ℹ️ [DriverMonthlyStatsService] No existen conductores activos en driver_active_list después del procesamiento");
            return Collections.emptyList();
        }

        return construirRespuestas(activos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverMonthlyStatsResponse> listarActivos() {
        log.info("📄 [DriverMonthlyStatsService] Listando conductores desde driver_active_list sin reprocesar");

        List<DriverActiveList> activos = driverActiveListRepository.findAll();
        if (activos.isEmpty()) {
            log.info("ℹ️ [DriverMonthlyStatsService] driver_active_list no contiene registros");
            return Collections.emptyList();
        }

        return construirRespuestas(activos);
    }

    private boolean esDriverIdValido(String driverId) {
        return driverId != null && !driverId.isBlank();
    }

    private int obtenerTrips(DriverMonthlyStats stat) {
        if (stat.getSumOrdersCompleted() != null) {
            return stat.getSumOrdersCompleted();
        }
        if (stat.getCountOrdersCompleted() != null) {
            return stat.getCountOrdersCompleted();
        }
        return 0;
    }

    private void actualizarActivo(DriverActiveList activo, DriverMonthlyStats stat, String categoria, int trips, String parkId) {
        activo.setParkId(parkId);
        activo.setTrips(trips);
        activo.setMonth(stat.getMonth());
        activo.setYear(stat.getYear());
        activo.setCategory(categoria);
        activo.setCountOrdersCompleted(stat.getCountOrdersCompleted());
        activo.setCountOrdersAll(stat.getCountOrdersAll());
        activo.setCountOrdersAccepted(stat.getCountOrdersAccepted());
        activo.setCountOrdersCancelledByClient(stat.getCountOrdersCancelledByClient());
        activo.setCountOrdersCancelledByDriver(stat.getCountOrdersCancelledByDriver());
        activo.setCountOrdersPlatform(stat.getCountOrdersPlatform());
        activo.setSumPriceCash(stat.getSumPriceCash());
        activo.setSumPriceCashless(stat.getSumPriceCashless());
        activo.setSumPriceOtherGas(stat.getSumPriceOtherGas());
        activo.setSumPriceParkCommission(stat.getSumPriceParkCommission());
        activo.setSumPricePlatformCommission(stat.getSumPricePlatformCommission());
        activo.setSumWorkTimeSeconds(stat.getSumWorkTimeSeconds());
    }

    private List<DriverMonthlyStatsResponse> construirRespuestas(List<DriverActiveList> activos) {
        log.info("📦 [DriverMonthlyStatsService] Registros activos listados: {}", activos.size());

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
        String driverId = activo.getDriverId();
        String parkName = flotaLookupService.obtenerNombreFlota(activo.getParkId());

        String detalleCategoria = generarDetalleCategoria(activo, driver);

        return DriverMonthlyStatsResponse.builder()
                .id(activo.getId())
                .driverId(driverId)
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
                .categoryDetail(detalleCategoria)
                .hireDate(driver != null ? driver.getHireDate() : null)
                .build();
    }

    private void marcarSincronizacion(DriverMonthlyStats stat, boolean sincronizado, List<DriverMonthlyStats> acumulador) {
        if (sincronizado) {
            if (!Boolean.TRUE.equals(stat.getCategorySynced())) {
                stat.setCategorySynced(Boolean.TRUE);
                stat.setCategorySyncedAt(LocalDateTime.now(LIMA_ZONE));
                acumulador.add(stat);
            }
        } else {
            if (!Boolean.FALSE.equals(stat.getCategorySynced())) {
                stat.setCategorySynced(Boolean.FALSE);
                stat.setCategorySyncedAt(null);
                acumulador.add(stat);
            }
        }
    }

    private void eliminarActivoSiExiste(Map<String, DriverActiveList> activosPorDriver, String driverId, Set<Long> idsParaEliminar) {
        DriverActiveList existente = activosPorDriver.remove(driverId);
        if (existente != null && existente.getId() != null) {
            idsParaEliminar.add(existente.getId());
        }
    }

    private long calcularMesesDesdeVinculacion(LocalDate hireDate, YearMonth mesEvaluacion) {
        if (hireDate == null) {
            return -1;
        }
        YearMonth hireMonth = YearMonth.from(hireDate);
        if (hireMonth.isAfter(mesEvaluacion)) {
            return -1;
        }
        return hireMonth.until(mesEvaluacion, ChronoUnit.MONTHS) + 1;
    }

    private Optional<String> determinarCategoria(int trips, long mesesDesdeVinculacion) {
        if (trips >= 400) {
            return Optional.of("premiun oro");
        }
        if (mesesDesdeVinculacion <= 0) {
            return Optional.empty();
        }

        long mesEvaluado = Math.min(mesesDesdeVinculacion, 3);
        int umbral;
        if (mesEvaluado == 1) {
            umbral = 50;
        } else if (mesEvaluado == 2) {
            umbral = 100;
        } else {
            umbral = 200;
        }

        if (trips >= umbral) {
            return Optional.of("premiun plata");
        }
        return Optional.empty();
    }

    private String resolverParkId(DriverMonthlyStats stat, Driver driver) {
        if (esDriverIdValido(stat.getParkId())) {
            return stat.getParkId();
        }
        if (driver != null && esDriverIdValido(driver.getParkId())) {
            return driver.getParkId();
        }
        return null;
    }

    private YearMonth obtenerMesEvaluacion() {
        YearMonth actual = YearMonth.now(LIMA_ZONE);
        return actual.minusMonths(1);
    }

    private String generarDetalleCategoria(DriverActiveList activo, Driver driver) {
        YearMonth periodo = activo.getYear() != null && activo.getMonth() != null
                ? YearMonth.of(activo.getYear(), activo.getMonth())
                : null;
        LocalDate hireDate = driver != null ? driver.getHireDate() : null;

        if (periodo == null || hireDate == null) {
            return "Sin datos suficientes para detalle";
        }

        long meses = calcularMesesDesdeVinculacion(hireDate, periodo);
        if (meses <= 0) {
            return "Aún no cumple un mes completo desde la vinculación";
        }

        if ("premiun oro".equalsIgnoreCase(activo.getCategory())) {
            return String.format("Mes %d desde la vinculación, %d viajes ≥ 400 → premiun oro", meses, Optional.ofNullable(activo.getTrips()).orElse(0));
        }

        int umbral = meses == 1 ? 50 : meses == 2 ? 100 : 200;
        return String.format("Mes %d desde la vinculación, %d viajes (mínimo requerido %d) → %s",
                meses,
                Optional.ofNullable(activo.getTrips()).orElse(0),
                umbral,
                Optional.ofNullable(activo.getCategory()).orElse("premiun plata"));
    }
}

