package com.yego.backend.service.yego_premiun.impl;

import com.yego.backend.entity.yego_garantizado.entities.Driver;
import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;
import com.yego.backend.entity.yego_premiun.entities.DriverMonthlyStats;
import com.yego.backend.repository.yego_garantizado.DriverRepository;
import com.yego.backend.repository.yego_premiun.DriverMonthlyStatsRepository;
import com.yego.backend.service.yego_premiun.DriverMonthlyStatsService;
import com.yego.backend.service.yego_premiun.FlotaLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    private final DriverMonthlyStatsRepository driverMonthlyStatsRepository;
    private final DriverRepository driverRepository;
    private final FlotaLookupService flotaLookupService;

    @Override
    @Transactional
    public List<DriverMonthlyStatsResponse> obtenerEstadisticas() {
        log.info("📊 [DriverMonthlyStatsService] Iniciando consulta de estadísticas mensuales de conductores");

        List<DriverMonthlyStats> stats = driverMonthlyStatsRepository.findAll();
        if (stats.isEmpty()) {
            log.info("ℹ️ [DriverMonthlyStatsService] No se encontraron registros en driver_monthly_stats");
            return Collections.emptyList();
        }

        Set<String> driverIds = stats.stream()
                .map(DriverMonthlyStats::getDriverId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Driver> driverMap = driverIds.isEmpty()
                ? Collections.emptyMap()
                : driverRepository.findAllById(driverIds).stream()
                .collect(Collectors.toMap(Driver::getDriverId, Function.identity()));

        log.info("📦 [DriverMonthlyStatsService] Registros encontrados: {}, conductores asociados: {}",
                stats.size(), driverMap.size());

        List<DriverMonthlyStats> statsActualizados = new ArrayList<>();
        List<DriverMonthlyStatsResponse> respuesta = stats.stream()
                .map(stat -> buildResponse(stat, driverMap.get(stat.getDriverId()), statsActualizados))
                .filter(response -> esCategoriaPermitida(response.getCategory()))
                .collect(Collectors.toList());

        if (!statsActualizados.isEmpty()) {
            driverMonthlyStatsRepository.saveAll(statsActualizados);
            log.info("📝 [DriverMonthlyStatsService] Categorías actualizadas para {} registros", statsActualizados.size());
        }

        return respuesta;
    }

    private DriverMonthlyStatsResponse buildResponse(DriverMonthlyStats stat, Driver driver, List<DriverMonthlyStats> statsActualizados) {
        Integer trips = Optional.ofNullable(stat.getSumOrdersCompleted())
                .or(() -> Optional.ofNullable(stat.getCountOrdersCompleted()))
                .orElse(0);

        String nuevaCategoria = determinarCategoria(trips);
        boolean requiereActualizacion = !Objects.equals(stat.getCategory(), nuevaCategoria)
                || Boolean.FALSE.equals(stat.getCategorySynced());

        if (requiereActualizacion) {
            stat.setCategory(nuevaCategoria);
            stat.setCategorySynced(Boolean.TRUE);
            stat.setCategorySyncedAt(ZonedDateTime.now(ZoneId.of("America/Lima")).toLocalDateTime());
            statsActualizados.add(stat);
        }

        return DriverMonthlyStatsResponse.builder()
                .id(stat.getId())
                .driverId(stat.getDriverId())
                .parkName(flotaLookupService.obtenerNombreFlota(stat.getParkId()))
                .month(stat.getMonth())
                .year(stat.getYear())
                .category(nuevaCategoria)
                .countOrdersCompleted(stat.getCountOrdersCompleted())
                .countOrdersAll(stat.getCountOrdersAll())
                .countOrdersAccepted(stat.getCountOrdersAccepted())
                .countOrdersCancelledByClient(stat.getCountOrdersCancelledByClient())
                .countOrdersCancelledByDriver(stat.getCountOrdersCancelledByDriver())
                .countOrdersPlatform(stat.getCountOrdersPlatform())
                .countActiveDrivers(stat.getCountActiveDrivers())
                .countDrivers(stat.getCountDrivers())
                .acceptanceRate(stat.getAcceptanceRate())
                .completionRate(stat.getCompletionRate())
                .sumDistance(stat.getSumDistance())
                .sumOrdersCompleted(stat.getSumOrdersCompleted())
                .sumPriceCash(stat.getSumPriceCash())
                .sumPriceCashless(stat.getSumPriceCashless())
                .sumPriceOtherGas(stat.getSumPriceOtherGas())
                .sumPriceParkCommission(stat.getSumPriceParkCommission())
                .sumPricePlatformCommission(stat.getSumPricePlatformCommission())
                .sumWorkTimeSeconds(stat.getSumWorkTimeSeconds())
                .tripsPerHour(stat.getTripsPerHour())
                .createdAt(stat.getCreatedAt())
                .fullName(driver != null ? driver.getFullName() : null)
                .phone(driver != null ? driver.getPhone() : null)
                .licenseNumber(driver != null ? driver.getLicenseNumber() : null)
                .categorySynced(stat.getCategorySynced())
                .categorySyncedAt(stat.getCategorySyncedAt())
                .build();
    }

    private String determinarCategoria(Integer trips) {
        if (trips == null || trips < 100) {
            return "no cumple con viajes minimos";
        }
        if (trips >= 400) {
            return "oro";
        }
        if (trips >= 200) {
            return "bronce";
        }
        if (trips >= 100) {
            return "plata";
        }
        return "no cumple con viajes minimos";
    }

    private boolean esCategoriaPermitida(String categoria) {
        return "oro".equalsIgnoreCase(categoria)
                || "plata".equalsIgnoreCase(categoria)
                || "bronce".equalsIgnoreCase(categoria);
    }
}

