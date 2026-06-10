package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.RendimientoResponse;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.PaymentPercentageRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.TripRepository;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import com.yego.backend.service.yego_pro_ops.RendimientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RendimientoServiceImpl implements RendimientoService {

    private final ShiftSessionRepository shiftSessionRepository;
    private final TripRepository tripRepository;
    private final DriverCloseRepository driverCloseRepository;
    private final FleetDriverService fleetDriverService;
    private final PaymentPercentageRepository paymentPercentageRepository;

    @Override
    public RendimientoResponse getRendimiento(String periodo, LocalDate weekStart, Integer mes, Integer anio) {
        LocalDate desde;
        LocalDate hasta;

        if ("semanal".equals(periodo) && weekStart != null) {
            desde = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            hasta = desde.plusDays(6);
        } else if ("mensual".equals(periodo) && mes != null && anio != null) {
            desde = LocalDate.of(anio, mes, 1);
            hasta = desde.withDayOfMonth(desde.lengthOfMonth());
        } else {
            desde = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            hasta = desde.plusDays(6);
        }

        log.info("[RendimientoService] periodo={} desde={} hasta={}", periodo, desde, hasta);

        List<ShiftSession> sessions = shiftSessionRepository.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .filter(s -> s.getStartedAt() != null)
                .filter(s -> s.getClosedAt() != null)
                .filter(s -> {
                    LocalDate startDate = s.getStartedAt().toLocalDate();
                    LocalDate endDate = s.getClosedAt().toLocalDate();
                    return !startDate.isAfter(hasta) && !endDate.isBefore(desde);
                })
                .collect(Collectors.toList());

        Set<String> driverIds = sessions.stream().map(ShiftSession::getDriverId).collect(Collectors.toSet());

        List<DriverClose> cierres = driverCloseRepository.findAll().stream()
                .filter(dc -> driverIds.contains(dc.getDriverId()))
                .filter(dc -> dc.getFecha() != null && !dc.getFecha().isBefore(desde) && !dc.getFecha().isAfter(hasta))
                .collect(Collectors.toList());

        Map<String, List<ShiftSession>> sessionsByDriver = sessions.stream()
                .collect(Collectors.groupingBy(ShiftSession::getDriverId));

        Map<String, List<DriverClose>> cierresByDriver = cierres.stream()
                .collect(Collectors.groupingBy(DriverClose::getDriverId));

        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalYape = BigDecimal.ZERO;
        BigDecimal totalKm = BigDecimal.ZERO;
        BigDecimal totalGnv = BigDecimal.ZERO;
        BigDecimal totalGasolina = BigDecimal.ZERO;
        BigDecimal totalHoras = BigDecimal.ZERO;
        BigDecimal producidoTotal = BigDecimal.ZERO;
        int totalViajes = 0;
        int totalConductores = driverIds.size();

        List<RendimientoResponse.ConductorRendimiento> conductores = new ArrayList<>();
        Map<String, String> driverNames = new HashMap<>();
        try {
            DriverSimpleResponse lista = fleetDriverService.obtenerListaConductoresSimplificada();
            if (lista != null && lista.getConductores() != null) {
                for (DriverSimpleResponse.DriverInfo d : lista.getConductores()) {
                    driverNames.put(d.getDriverId(), d.getNombre() != null ? d.getNombre() : d.getDriverId());
                }
            }
        } catch (Exception e) {
            log.warn("[RendimientoService] no se pudieron resolver nombres de conductores: {}", e.getMessage());
        }

        for (String driverId : driverIds) {
            List<ShiftSession> driverSessions = sessionsByDriver.getOrDefault(driverId, List.of());
            List<DriverClose> driverCierres = cierresByDriver.getOrDefault(driverId, List.of());

            int viajes = driverSessions.stream().mapToInt(s -> s.getTotalTrips() != null ? s.getTotalTrips() : 0).sum();
            BigDecimal efectivo = driverCierres.stream()
                    .map(dc -> (dc.getLiquidaEfectivo() != null ? dc.getLiquidaEfectivo() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal yape = driverCierres.stream()
                    .map(dc -> (dc.getLiquidaYape() != null ? dc.getLiquidaYape() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal producido = driverSessions.stream()
                    .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal km = driverCierres.stream()
                    .map(dc -> dc.getDiferenciaOdometro() != null ? BigDecimal.valueOf(dc.getDiferenciaOdometro()) : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gnvSoles = driverCierres.stream()
                    .map(dc -> (dc.getGnvSoles() != null ? dc.getGnvSoles() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gasolinaSoles = driverCierres.stream()
                    .map(dc -> (dc.getGasolinaSoles() != null ? dc.getGasolinaSoles() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal otrosGastos = driverCierres.stream()
                    .map(dc -> (dc.getOtrosGastos() != null ? dc.getOtrosGastos() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long minutos = driverSessions.stream()
                    .filter(s -> s.getStartedAt() != null && s.getClosedAt() != null)
                    .mapToLong(s -> java.time.Duration.between(s.getStartedAt(), s.getClosedAt()).toMinutes())
                    .sum();
            BigDecimal horas = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            BigDecimal viajesPorHora = horas.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(viajes).divide(horas, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal totalGastosConductor = gnvSoles.add(gasolinaSoles).add(otrosGastos);
            BigDecimal rentabilidad = totalGastosConductor.compareTo(BigDecimal.ZERO) > 0
                    ? producido.divide(totalGastosConductor, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            totalEfectivo = totalEfectivo.add(efectivo);
            totalYape = totalYape.add(yape);
            totalKm = totalKm.add(km);
            totalGnv = totalGnv.add(gnvSoles);
            totalGasolina = totalGasolina.add(gasolinaSoles);
            totalHoras = totalHoras.add(horas);
            producidoTotal = producidoTotal.add(producido);
            totalViajes += viajes;

            conductores.add(RendimientoResponse.ConductorRendimiento.builder()
                    .driverId(driverId)
                    .nombre(driverNames.getOrDefault(driverId, driverId))
                    .totalViajes(viajes)
                    .totalEfectivo(efectivo)
                    .totalYape(yape)
                    .totalProducido(producido)
                    .totalKm(km)
                    .totalGnvSoles(gnvSoles)
                    .totalGasolinaSoles(gasolinaSoles)
                    .totalOtrosGastos(otrosGastos)
                    .totalHoras(horas)
                    .viajesPorHora(viajesPorHora)
                    .rentabilidad(rentabilidad)
                    .build());
        }

        conductores.sort((a, b) -> Integer.compare(b.getTotalViajes(), a.getTotalViajes()));

        BigDecimal viajesPorHoraTotal = totalHoras.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(totalViajes).divide(totalHoras, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        RendimientoResponse.TotalesRendimiento totales = RendimientoResponse.TotalesRendimiento.builder()
                .conductores(totalConductores)
                .viajes(totalViajes)
                .efectivo(totalEfectivo)
                .yape(totalYape)
                .montoTotalProducido(producidoTotal)
                .km(totalKm)
                .gnvSoles(totalGnv)
                .gasolinaSoles(totalGasolina)
                .horas(totalHoras)
                .viajesPorHora(viajesPorHoraTotal)
                .minimoViajes(paymentPercentageRepository.findApplicableForDate(LocalDate.now()).stream()
                        .mapToInt(PaymentPercentage::getMinValidatedTrips)
                        .min().orElse(0))
                .build();

        return RendimientoResponse.builder()
                .periodo(periodo)
                .desde(desde)
                .hasta(hasta)
                .totales(totales)
                .conductores(conductores)
                .build();
    }
}
