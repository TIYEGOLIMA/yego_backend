package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.LiquidarRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionPendienteResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionPendienteResponse.DiaPendienteInfo;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse.DiaLiquidacionInfo;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.SesionDiaInfo;
import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.Trip;
import com.yego.backend.repository.yego_pro_ops.BonusThresholdRepository;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.FacturacionSemanalRepository;
import com.yego.backend.repository.yego_pro_ops.PaymentPercentageRepository;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.TripRepository;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.LiquidacionService;
import com.yego.backend.service.yego_pro_ops.ShiftSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LiquidacionServiceImpl implements LiquidacionService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String DEFAULT_PARK_ID = "64085dd85e124e2c808806f70d527ea8";
    private static final BigDecimal TASA_MANTENIMIENTO = BigDecimal.valueOf(0.15);

    private final ShiftSessionRepository shiftSessionRepository;
    private final TripRepository tripRepository;
    private final ShiftSessionService shiftSessionService;
    private final DriverOrdersService driverOrdersService;
    private final FacturacionSemanalRepository facturacionSemanalRepository;
    private final BonusThresholdRepository bonusThresholdRepository;
    private final PaymentPercentageRepository paymentPercentageRepository;
    private final DriverCloseRepository driverCloseRepository;

    public LiquidacionServiceImpl(
            ShiftSessionRepository shiftSessionRepository,
            TripRepository tripRepository,
            ShiftSessionService shiftSessionService,
            DriverOrdersService driverOrdersService,
            FacturacionSemanalRepository facturacionSemanalRepository,
            BonusThresholdRepository bonusThresholdRepository,
            PaymentPercentageRepository paymentPercentageRepository,
            DriverCloseRepository driverCloseRepository) {
        this.shiftSessionRepository = shiftSessionRepository;
        this.tripRepository = tripRepository;
        this.shiftSessionService = shiftSessionService;
        this.driverOrdersService = driverOrdersService;
        this.facturacionSemanalRepository = facturacionSemanalRepository;
        this.bonusThresholdRepository = bonusThresholdRepository;
        this.paymentPercentageRepository = paymentPercentageRepository;
        this.driverCloseRepository = driverCloseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionSemanalResponse getLiquidacionSemanal(String driverId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<ShiftSession> sessions = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId).stream()
                .filter(s -> {
                    LocalDate startDate = s.getStartedAt().toLocalDate();
                    LocalDate endDate = s.getClosedAt() != null ? s.getClosedAt().toLocalDate() : LocalDate.now();
                    return !startDate.isAfter(weekEnd) && !endDate.isBefore(weekStart);
                })
                .sorted(Comparator.comparing(ShiftSession::getStartedAt))
                .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            return buildEmptySemanal(driverId, weekStart, weekEnd);
        }

        List<UUID> sessionIds = sessions.stream().map(ShiftSession::getId).collect(Collectors.toList());
        List<Trip> allTrips = tripRepository.findByShiftSessionIdIn(sessionIds);

        Map<LocalDate, List<ShiftSession>> sessionsByDay = new LinkedHashMap<>();
        for (ShiftSession s : sessions) {
            LocalDate dia = s.getStartedAt().toLocalDate();
            if (!dia.isBefore(weekStart) && !dia.isAfter(weekEnd)) {
                sessionsByDay.computeIfAbsent(dia, k -> new ArrayList<>()).add(s);
            }
        }

        boolean tieneActiva = sessions.stream().anyMatch(s -> "active".equals(s.getStatus()));
        boolean tieneCerrada = sessions.stream().anyMatch(s -> "completada".equals(s.getStatus()));

        LocalDateTime lastSettled = shiftSessionRepository.findLastSettledAtByDriverId(driverId).orElse(null);

        Map<UUID, BigDecimal> tripsPorSession = allTrips.stream()
                .collect(Collectors.groupingBy(Trip::getShiftSessionId,
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO,
                                BigDecimal::add)));

        BigDecimal totalPendiente = sessions.stream()
                .filter(s -> "completada".equals(s.getStatus())
                        && (lastSettled == null || (s.getClosedAt() != null && !s.getClosedAt().isBefore(lastSettled))))
                .map(s -> tripsPorSession.getOrDefault(s.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DiaLiquidacionInfo> dias = new ArrayList<>();
        int totalViajes = allTrips.size();
        BigDecimal totalKm = allTrips.stream()
                .map(t -> t.getDistanceKm() != null ? t.getDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            List<ShiftSession> sesionesDia = sessionsByDay.getOrDefault(d, List.of());

            int viajesDia = 0;
            BigDecimal ingresosDia = BigDecimal.ZERO;
            BigDecimal kmDia = BigDecimal.ZERO;
            BigDecimal ingresosPendientes = BigDecimal.ZERO;
            BigDecimal ingresosLiquidados = BigDecimal.ZERO;

            List<SesionDiaInfo> sesionesDetalle = new ArrayList<>();

            for (ShiftSession s : sesionesDia) {
                List<Trip> tripsSesion = allTrips.stream()
                        .filter(t -> t.getShiftSessionId().equals(s.getId()))
                        .collect(Collectors.toList());

                int v = tripsSesion.size();
                BigDecimal ing = tripsSesion.stream().map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal k = tripsSesion.stream().map(t -> t.getDistanceKm() != null ? t.getDistanceKm() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

                viajesDia += v;
                ingresosDia = ingresosDia.add(ing);
                kmDia = kmDia.add(k);

                if ("completada".equals(s.getStatus())
                        && (lastSettled == null || (s.getClosedAt() != null && !s.getClosedAt().isBefore(lastSettled)))) {
                    ingresosPendientes = ingresosPendientes.add(ing);
                } else if ("settled".equals(s.getStatus())) {
                    ingresosLiquidados = ingresosLiquidados.add(ing);
                }

                sesionesDetalle.add(SesionDiaInfo.builder()
                        .sessionId(s.getId())
                        .inicio(s.getStartedAt() != null ? s.getStartedAt().format(DATETIME_FORMATTER) : null)
                        .fin(s.getClosedAt() != null ? s.getClosedAt().format(DATETIME_FORMATTER) : null)
                        .viajes(v)
                        .ingresos(ing)
                        .km(k)
                        .status(s.getStatus())
                        .build());
            }

            String estado;
            if (sesionesDia.isEmpty()) {
                estado = "Sin actividad";
            } else {
                boolean hayActiva = sesionesDia.stream().anyMatch(s -> "active".equals(s.getStatus()));
                boolean hayPendienteValidacion = sesionesDia.stream().anyMatch(s ->
                    "por_validar".equals(s.getStatus())
                        && (lastSettled == null || (s.getClosedAt() != null && !s.getClosedAt().isBefore(lastSettled)))
                );
                boolean haySettled = sesionesDia.stream().anyMatch(s -> "settled".equals(s.getStatus()));
                boolean todasSettled = sesionesDia.stream().allMatch(s -> "settled".equals(s.getStatus()) || "completada".equals(s.getStatus()));

                if (todasSettled) {
                    estado = "Liquidado";
                } else if (hayActiva) {
                    estado = "En curso";
                } else if (hayPendienteValidacion && haySettled) {
                    estado = "Pendiente parcial";
                } else if (hayPendienteValidacion) {
                    estado = "Por validar";
                } else if (haySettled) {
                    estado = "Liquidado";
                } else {
                    estado = "Sin actividad";
                }
            }

            dias.add(DiaLiquidacionInfo.builder()
                    .fecha(d)
                    .diaSemana(d.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "PE")))
                    .viajes(viajesDia)
                    .ingresos(ingresosDia)
                    .ingresosPendientes(ingresosPendientes)
                    .ingresosLiquidados(ingresosLiquidados)
                    .km(kmDia)
                    .sesiones(sesionesDia.size())
                    .estado(estado)
                    .sesionesDetalle(sesionesDetalle)
                    .build());
        }

        String primerViaje = allTrips.stream()
                .map(Trip::getCompletedAt)
                .min(Comparator.naturalOrder())
                .map(DATETIME_FORMATTER::format)
                .orElse(null);

        String ultimoViaje = allTrips.stream()
                .map(Trip::getCompletedAt)
                .max(Comparator.naturalOrder())
                .map(DATETIME_FORMATTER::format)
                .orElse(null);

        List<UUID> sesionesPendientes = sessions.stream()
                .filter(s -> "completada".equals(s.getStatus())
                        && (lastSettled == null || (s.getClosedAt() != null && !s.getClosedAt().isBefore(lastSettled))))
                .map(ShiftSession::getId)
                .collect(Collectors.toList());

        BigDecimal montoTotalProducido = BigDecimal.ZERO;
        BigDecimal comisionApp = BigDecimal.ZERO;
        BigDecimal bonoYango = BigDecimal.ZERO;
        BigDecimal kmYango = BigDecimal.ZERO;
        int totalViajesYango = 0;
        BigDecimal viajesPorHora = BigDecimal.ZERO;

        Set<String> sessionTripIds = allTrips.stream()
                .map(Trip::getExternalTripId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        try {
            String dateFrom = weekStart.format(DATE_FORMATTER) + "T00:00:00-05:00";
            String dateTo = weekEnd.format(DATE_FORMATTER) + "T23:59:59-05:00";
            DriverOrdersResponse yango = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
            if (yango != null && yango.getOrders() != null) {
                List<OrderInfoResponse> filtradas = yango.getOrders().stream()
                        .filter(o -> o.getId() != null && sessionTripIds.contains(o.getId()))
                        .collect(Collectors.toList());
                if (!filtradas.isEmpty()) {
                    DatosYango dy = procesarDatosYango(filtradas);
                    montoTotalProducido = dy.montoTotalProducido;
                    comisionApp = dy.comisionApp;
                    bonoYango = dy.bonoYango;
                    kmYango = dy.km;
                    totalViajesYango = dy.totalViajes;
                }
            }
        } catch (Exception e) {
            log.warn("[LiquidacionService] error consultando Yango para getLiquidacionSemanal driverId={}: {}", driverId, e.getMessage());
        }

        List<DriverClose> cierresSemana = driverCloseRepository.findByDriverIdAndFechaBetween(driverId, weekStart, weekEnd);
        BigDecimal producidoCierres = cierresSemana.stream()
                .map(c -> nz(c.getMontoTotalProducido()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (producidoCierres.compareTo(BigDecimal.ZERO) > 0) {
            montoTotalProducido = producidoCierres;
        }
        BigDecimal gastoCombustible = cierresSemana.stream()
                .map(c -> nz(c.getGnvSoles()).add(nz(c.getGasolinaSoles())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<UUID, BigDecimal> producidoPorSesion = cierresSemana.stream()
                .filter(c -> c.getShiftSessionId() != null && c.getMontoTotalProducido() != null)
                .collect(Collectors.toMap(DriverClose::getShiftSessionId, DriverClose::getMontoTotalProducido, (a, b) -> b));

        BigDecimal kmFinal = kmYango.compareTo(BigDecimal.ZERO) > 0 ? kmYango : totalKm;
        BigDecimal gastoMantenimiento = kmFinal.multiply(TASA_MANTENIMIENTO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoNeto = montoTotalProducido.subtract(comisionApp);
        if (montoNeto.compareTo(BigDecimal.ZERO) < 0) montoNeto = BigDecimal.ZERO;

        BigDecimal produccionBonificable = montoNeto.add(bonoYango).subtract(gastoCombustible).subtract(gastoMantenimiento);
        if (produccionBonificable.compareTo(BigDecimal.ZERO) < 0) produccionBonificable = BigDecimal.ZERO;

        int viajesParaCalc = Math.max(totalViajes, totalViajesYango);
        BigDecimal bonoAdicViajes = calcularBono(viajesParaCalc);
        BigDecimal bono = produccionBonificable.subtract(bonoAdicViajes);
        if (bono.compareTo(BigDecimal.ZERO) < 0) bono = BigDecimal.ZERO;

        Double porcentajePago = calcularPorcentajePago(viajesParaCalc);
        BigDecimal pago = bono.multiply(BigDecimal.valueOf(porcentajePago)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pagoTotal = pago.add(bonoAdicViajes);
        BigDecimal utilidad = pagoTotal;
        BigDecimal utilidadPorViaje = viajesParaCalc > 0
                ? utilidad.divide(BigDecimal.valueOf(viajesParaCalc), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal pagoPorViaje = viajesParaCalc > 0
                ? pagoTotal.divide(BigDecimal.valueOf(viajesParaCalc), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal horasEfectivasSemanal = calcularHorasEfectivas(sessions);
        viajesPorHora = horasEfectivasSemanal.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(viajesParaCalc).divide(horasEfectivasSemanal, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        java.util.Set<UUID> visto = new java.util.HashSet<>();
        List<SesionDiaInfo> sesionesUnicas = new ArrayList<>();
        for (ShiftSession s : sessions) {
            if (!visto.add(s.getId())) continue;
            List<Trip> tripsSesion = allTrips.stream().filter(t -> t.getShiftSessionId().equals(s.getId())).collect(Collectors.toList());
            BigDecimal efectivoSesion = nz(s.getTotalCash());
            BigDecimal producidoSesion = producidoPorSesion.getOrDefault(s.getId(), BigDecimal.ZERO);
            sesionesUnicas.add(SesionDiaInfo.builder()
                    .sessionId(s.getId())
                    .inicio(s.getStartedAt() != null ? s.getStartedAt().format(DATETIME_FORMATTER) : null)
                    .fin(s.getClosedAt() != null ? s.getClosedAt().format(DATETIME_FORMATTER) : null)
                    .viajes(tripsSesion.size())
                    .ingresos(efectivoSesion)
                    .efectivo(efectivoSesion)
                    .montoTotalProducido(producidoSesion)
                    .km(tripsSesion.stream().map(t -> t.getDistanceKm() != null ? t.getDistanceKm() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .status(s.getStatus())
                    .build());
        }

        boolean semanaCerradaSemanal = facturacionSemanalRepository
                .existsOverlappingWithDriver(driverId, weekStart, weekEnd);

        BigDecimal bonificacionEmpresa = null;
        BigDecimal pagoTotalFinal = null;
        BigDecimal totalAdelantos = BigDecimal.ZERO;
        BigDecimal pagoTotalConAdelantos = null;
        if (semanaCerradaSemanal) {
            Optional<FacturacionSemanal> facturacionExistente = facturacionSemanalRepository
                    .findByDriverIdAndFechaInicioAndFechaFin(driverId, weekStart, weekEnd);
            if (facturacionExistente.isPresent()) {
                bonificacionEmpresa = facturacionExistente.get().getBonificacionEmpresa();
                pagoTotalFinal = facturacionExistente.get().getPagoTotalFinal();
                totalAdelantos = facturacionExistente.get().getTotalAdelantos() != null
                        ? facturacionExistente.get().getTotalAdelantos() : BigDecimal.ZERO;
                pagoTotalConAdelantos = facturacionExistente.get().getPagoTotalConAdelantos();
            }
        }

        // Calcular total adelantos de los cierres de la semana
        if (totalAdelantos.compareTo(BigDecimal.ZERO) == 0 && !semanaCerradaSemanal) {
            List<DriverClose> cierresAdelantos = driverCloseRepository
                    .findByDriverIdAndFechaBetween(driverId, weekStart, weekEnd);
            totalAdelantos = cierresAdelantos.stream()
                    .map(c -> c.getAdelanto() != null ? c.getAdelanto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalAdelantos.compareTo(BigDecimal.ZERO) > 0) {
                pagoTotalConAdelantos = pagoTotal.subtract(totalAdelantos);
            }
        }

        return LiquidacionSemanalResponse.builder()
                .driverId(driverId).semanaInicio(weekStart).semanaFin(weekEnd)
                .totalSesiones(sessions.size()).totalViajes(totalViajes).totalIngresos(totalPendiente).totalKm(totalKm)
                .primerViaje(primerViaje).ultimoViaje(ultimoViaje).dias(dias)
                .sesionesPendientes(sesionesPendientes).tieneSesionesCerradas(tieneCerrada).tieneSesionActiva(tieneActiva)
                .montoTotalProducido(montoTotalProducido).bonoYango(bonoYango).comisionApp(comisionApp)
                .montoNeto(montoNeto).produccionBonificable(produccionBonificable).bonoAdicViajes(bonoAdicViajes)
                .bono(bono).porcentajePago(porcentajePago).pago(pago).pagoTotal(pagoTotal)
                .bonificacionEmpresa(bonificacionEmpresa).pagoTotalFinal(pagoTotalFinal)
                .totalAdelantos(totalAdelantos).pagoTotalConAdelantos(pagoTotalConAdelantos)
                .utilidad(utilidad).utilidadPorViaje(utilidadPorViaje).pagoPorViaje(pagoPorViaje)
                .kmRecorrido(kmFinal).gastoMantenimiento(gastoMantenimiento).gastoCombustible(gastoCombustible).viajesPorHora(viajesPorHora).sesionesDetalle(sesionesUnicas).semanaCerrada(semanaCerradaSemanal)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidacionPendienteResponse getLiquidacionPendiente(String driverId, LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            LocalDateTime ultimoCierre = shiftSessionRepository.findLastClosedAtByDriverId(driverId).orElse(null);
            if (ultimoCierre != null) {
                desde = ultimoCierre.plusMinutes(1);
            } else {
                List<ShiftSession> todas = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId);
                desde = todas.stream().map(ShiftSession::getStartedAt).min(Comparator.naturalOrder())
                        .orElse(LocalDateTime.now().minusDays(30));
            }
            hasta = LocalDateTime.now();
        }
        final LocalDateTime desdeFinal = desde;
        final LocalDateTime hastaFinal = hasta;
        boolean esPrimera = shiftSessionRepository.findLastSettledAtByDriverId(driverId).isEmpty();

        List<ShiftSession> sessionsCerradas = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId).stream()
                .filter(s -> "completada".equals(s.getStatus()))
                .filter(s -> s.getClosedAt() != null && !s.getClosedAt().isBefore(desdeFinal))
                .filter(s -> s.getStartedAt() != null && !s.getStartedAt().isAfter(hastaFinal))
                .sorted(Comparator.comparing(ShiftSession::getStartedAt))
                .collect(Collectors.toList());

        List<UUID> sessionIds = sessionsCerradas.stream().map(ShiftSession::getId).collect(Collectors.toList());
        List<Trip> allTrips = new ArrayList<>();
        for (UUID sid : sessionIds) {
            allTrips.addAll(tripRepository.findByShiftSessionId(sid));
        }

        int totalViajesLocales = allTrips.size();
        BigDecimal totalKmLocales = allTrips.stream()
                .map(t -> t.getDistanceKm() != null ? t.getDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal horasEfectivas = calcularHorasEfectivas(sessionsCerradas);

        DatosYango datosYango = null;
        String placa = "";
        String carBrandModel = "";
        try {
            String dateFrom = desdeFinal.format(ISO_DATETIME_FORMATTER) + "-05:00";
            String dateTo = hastaFinal.format(ISO_DATETIME_FORMATTER) + "-05:00";
            DriverOrdersResponse yango = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
            if (yango != null && yango.getOrders() != null && !yango.getOrders().isEmpty()) {
                datosYango = procesarDatosYango(yango.getOrders());
                carBrandModel = yango.getOrders().get(0).getCarBrandModel() != null ? yango.getOrders().get(0).getCarBrandModel() : "";
                placa = yango.getOrders().get(0).getCarLicenseNumber() != null ? yango.getOrders().get(0).getCarLicenseNumber() : "";
            }
        } catch (Exception e) {
            log.warn("[LiquidacionService] error consultando Yango para getLiquidacionPendiente driverId={}: {}", driverId, e.getMessage());
        }

        int totalViajesYango = datosYango != null ? datosYango.totalViajes : 0;

        if (sessionsCerradas.isEmpty() && datosYango == null) {
            return buildEmptyPendiente(driverId, desdeFinal, hastaFinal, esPrimera);
        }

        int totalViajes = Math.max(totalViajesLocales, totalViajesYango);
        BigDecimal viajesPorHora = horasEfectivas.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(totalViajes).divide(horasEfectivas, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal montoTotalProducido = BigDecimal.ZERO;
        BigDecimal comisionApp = BigDecimal.ZERO;
        BigDecimal bonoYango = BigDecimal.ZERO;
        BigDecimal kmYango = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;

        if (datosYango != null) {
            montoTotalProducido = datosYango.montoTotalProducido;
            comisionApp = datosYango.comisionApp;
            bonoYango = datosYango.bonoYango;
            kmYango = datosYango.km;
            efectivo = datosYango.efectivo;
        }

        BigDecimal montoNeto = montoTotalProducido.subtract(comisionApp);
        if (montoNeto.compareTo(BigDecimal.ZERO) < 0) montoNeto = BigDecimal.ZERO;

        BigDecimal kmFinal = kmYango.compareTo(BigDecimal.ZERO) > 0 ? kmYango : totalKmLocales;
        BigDecimal pagoTotal = montoNeto;
        BigDecimal utilidad = pagoTotal;
        BigDecimal utilidadPorViaje = totalViajes > 0 ? utilidad.divide(BigDecimal.valueOf(totalViajes), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal pagoPorViaje = totalViajes > 0 ? pagoTotal.divide(BigDecimal.valueOf(totalViajes), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<LocalDate, List<ShiftSession>> sessionsByDay = new LinkedHashMap<>();
        for (ShiftSession s : sessionsCerradas) {
            LocalDate dia = s.getStartedAt().toLocalDate();
            sessionsByDay.computeIfAbsent(dia, k -> new ArrayList<>()).add(s);
        }

        List<DiaPendienteInfo> dias = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ShiftSession>> entry : sessionsByDay.entrySet()) {
            LocalDate fecha = entry.getKey();
            List<ShiftSession> sesionesDia = entry.getValue();
            int viajesDia = 0;
            BigDecimal ingresosDia = BigDecimal.ZERO;
            BigDecimal kmDia = BigDecimal.ZERO;
            List<SesionDiaInfo> sesionesDetalle = new ArrayList<>();
            for (ShiftSession s : sesionesDia) {
                List<Trip> tripsSesion = allTrips.stream().filter(t -> t.getShiftSessionId().equals(s.getId())).collect(Collectors.toList());
                int v = tripsSesion.size();
                BigDecimal ing = tripsSesion.stream().map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal k = tripsSesion.stream().map(t -> t.getDistanceKm() != null ? t.getDistanceKm() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                viajesDia += v; ingresosDia = ingresosDia.add(ing); kmDia = kmDia.add(k);
                sesionesDetalle.add(SesionDiaInfo.builder().sessionId(s.getId())
                        .inicio(s.getStartedAt() != null ? s.getStartedAt().format(DATETIME_FORMATTER) : null)
                        .fin(s.getClosedAt() != null ? s.getClosedAt().format(DATETIME_FORMATTER) : null)
                        .viajes(v).ingresos(ing).efectivo(nz(s.getTotalCash())).montoTotalProducido(ing).km(k).status(s.getStatus()).build());
            }
            dias.add(DiaPendienteInfo.builder().fecha(fecha.format(DATE_FORMATTER))
                    .diaSemana(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "PE")))
                    .sesiones(sesionesDia.size()).viajes(viajesDia).ingresos(ingresosDia).km(kmDia)
                    .estado("Cerrado").sesionesDetalle(sesionesDetalle).build());
        }

        List<UUID> sesionesPendientes = sessionsCerradas.stream().map(ShiftSession::getId).collect(Collectors.toList());

        boolean semanaCerrada = facturacionSemanalRepository
                .existsOverlappingWithDriver(driverId, desdeFinal.toLocalDate(), hastaFinal.toLocalDate());

        return LiquidacionPendienteResponse.builder()
                .driverId(driverId).periodoDesde(desdeFinal).periodoHasta(hastaFinal)
                .esPrimeraLiquidacion(esPrimera).totalSesiones(sessionsCerradas.size())
                .totalViajes(totalViajes).viajesPorHora(viajesPorHora).kmRecorrido(kmFinal)
                .montoTotalProducido(montoTotalProducido).placa(placa).carBrandModel(carBrandModel).semanaCerrada(semanaCerrada)
                .bonoYango(bonoYango).comisionApp(comisionApp)
                .montoNeto(montoNeto).produccionBonificable(montoNeto).bono(BigDecimal.ZERO)
                .porcentajePago(0.0).pago(BigDecimal.ZERO).pagoTotal(pagoTotal)
                .utilidad(utilidad).utilidadPorViaje(utilidadPorViaje).pagoPorViaje(pagoPorViaje)
                .diasTrabajados(sessionsByDay.size()).sesionesPendientes(sesionesPendientes).dias(dias)
                .efectivo(efectivo)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> liquidarPendiente(LiquidarRequest request) {
        String driverId = request.getDriverId();
        Long userId = request.getUserId();
        LocalDateTime desde = parseDateTimeLiquidar(request.getDesde());
        LocalDateTime hasta = parseDateTimeLiquidar(request.getHasta());
        if (desde == null || hasta == null) {
            LocalDateTime ultimoCierre = shiftSessionRepository.findLastClosedAtByDriverId(driverId).orElse(null);
            if (ultimoCierre != null) {
                desde = ultimoCierre.plusMinutes(1);
            } else {
                List<ShiftSession> todas = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId);
                desde = todas.stream().map(ShiftSession::getStartedAt).min(Comparator.naturalOrder())
                        .orElse(LocalDateTime.now().minusDays(30));
            }
            hasta = LocalDateTime.now();
        }
        final LocalDateTime desdeFinal = desde;
        final LocalDateTime hastaFinal = hasta;

        List<ShiftSession> sessionsCerradas = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId).stream()
                .filter(s -> "completada".equals(s.getStatus()))
                .filter(s -> s.getClosedAt() != null && !s.getClosedAt().isBefore(desdeFinal))
                .filter(s -> s.getStartedAt() != null && !s.getStartedAt().isAfter(hastaFinal))
                .collect(Collectors.toList());

        UUID sessionId = null;
        if (sessionsCerradas.isEmpty()) {
            String dateFrom = desdeFinal.format(ISO_DATETIME_FORMATTER) + "-05:00";
            String dateTo = hastaFinal.format(ISO_DATETIME_FORMATTER) + "-05:00";
            DriverOrdersResponse yango = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
            if (yango == null || yango.getOrders() == null || yango.getOrders().isEmpty()) {
                return Map.of("liquidado", false, "mensaje", "No se encontraron viajes en el período");
            }
            ShiftSession session = ShiftSession.builder()
                    .driverId(driverId).startedAt(desdeFinal).closedAt(hastaFinal)
                    .status("por_validar").totalTrips(yango.getOrders().size()).totalAmount(BigDecimal.ZERO)
                    .build();
            session = shiftSessionRepository.save(session);
            sessionId = session.getId();
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalCash = BigDecimal.ZERO;
            for (OrderInfoResponse order : yango.getOrders()) {
                BigDecimal amount = order.getPrice() != null ? BigDecimal.valueOf(order.getPrice()) : BigDecimal.ZERO;
                totalAmount = totalAmount.add(amount);
                if (order.getCash() != null) totalCash = totalCash.add(BigDecimal.valueOf(order.getCash()));
                Trip trip = Trip.builder()
                        .driverId(driverId).shiftSessionId(session.getId()).externalTripId(order.getId())
                        .completedAt(order.getEndedAt() != null ? LocalDateTime.parse(order.getEndedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : LocalDateTime.now())
                        .amount(amount).distanceKm(order.getDistance() != null ? BigDecimal.valueOf(order.getDistance()) : BigDecimal.ZERO)
                        .build();
                tripRepository.save(trip);
            }
            session.setTotalAmount(totalAmount);
            log.info("[LiquidacionService] DEBUG totalCash={} totalAmount={} ordersCount={}", totalCash, totalAmount, yango.getOrders().size());
            session.setTotalCash(totalCash);
            shiftSessionRepository.save(session);
            sessionsCerradas = List.of(session);
            log.info("[LiquidacionService] sesión auto-generada sessionId={} viajes={}", sessionId, yango.getOrders().size());
        }

        for (ShiftSession s : sessionsCerradas) {
            shiftSessionService.settleSession(s.getId(), userId);
            if (sessionId == null) sessionId = s.getId();

            double gnvSoles = request.getGnvSoles() != null ? request.getGnvSoles() : 0;
            double gasolinaSoles = request.getGasolinaSoles() != null ? request.getGasolinaSoles() : 0;
            double otrosGastos = request.getOtrosGastos() != null ? request.getOtrosGastos() : 0;
            double totalIngresos = s.getTotalCash() != null ? s.getTotalCash().doubleValue() : 0;
            double totalGastos = gnvSoles + gasolinaSoles + otrosGastos;
            BigDecimal producidoSesion = calcularProducidoYango(driverId, s.getStartedAt(), s.getClosedAt());

            DriverClose cierre = DriverClose.builder()
                    .driverId(driverId).fecha(s.getStartedAt().toLocalDate()).userId(userId)
                    .shiftSessionId(s.getId())
                    .placa(request.getPlaca())
                    .odometroInicial(request.getOdometroInicial()).odometroFinal(request.getOdometroFinal())
                    .diferenciaOdometro(request.getDiferenciaOdometro())
                    .gnvM3(request.getGnvM3()).gnvSoles(BigDecimal.valueOf(gnvSoles))
                    .gasolinaGalones(request.getGasolinaGalones()).gasolinaSoles(BigDecimal.valueOf(gasolinaSoles))
                    .liquidaEfectivo(BigDecimal.valueOf(request.getLiquidaEfectivo() != null ? request.getLiquidaEfectivo() : 0))
                    .liquidaYape(BigDecimal.valueOf(request.getLiquidaYape() != null ? request.getLiquidaYape() : 0))
                    .otrosGastos(BigDecimal.valueOf(otrosGastos))
                    .otrosGastosDescripcion(request.getOtrosGastosDescripcion())
                    .totalIngresos(BigDecimal.valueOf(totalIngresos)).totalGastos(BigDecimal.valueOf(totalGastos))
                    .resta(BigDecimal.valueOf(totalIngresos - totalGastos))
                    .montoTotalProducido(producidoSesion)
                    .build();
            Optional<DriverClose> existente = driverCloseRepository
                    .findFirstByDriverIdAndFechaOrderByIdDesc(driverId, s.getStartedAt().toLocalDate());
            if (existente.isPresent()) {
                DriverClose actual = existente.get();
                actual.setGnvM3(cierre.getGnvM3());
                actual.setGnvSoles(cierre.getGnvSoles());
                actual.setGasolinaGalones(cierre.getGasolinaGalones());
                actual.setGasolinaSoles(cierre.getGasolinaSoles());
                actual.setLiquidaEfectivo(cierre.getLiquidaEfectivo());
                actual.setLiquidaYape(cierre.getLiquidaYape());
                actual.setOtrosGastos(cierre.getOtrosGastos());
                actual.setOtrosGastosDescripcion(cierre.getOtrosGastosDescripcion());
                actual.setTotalIngresos(cierre.getTotalIngresos());
                actual.setTotalGastos(cierre.getTotalGastos());
                actual.setResta(cierre.getResta());
                actual.setMontoTotalProducido(cierre.getMontoTotalProducido());
                actual.setPlaca(cierre.getPlaca());
                actual.setOdometroInicial(cierre.getOdometroInicial());
                actual.setOdometroFinal(cierre.getOdometroFinal());
                actual.setDiferenciaOdometro(cierre.getDiferenciaOdometro());
                actual.setShiftSessionId(cierre.getShiftSessionId());
                actual.setUserId(cierre.getUserId());
                driverCloseRepository.save(actual);
                log.info("[LiquidacionService] cierre actualizado sessionId={} placa={} ingresos={} gastos={}",
                        s.getId(), request.getPlaca(), totalIngresos, totalGastos);
            } else {
                driverCloseRepository.save(cierre);
                log.info("[LiquidacionService] cierre registrado sessionId={} placa={} ingresos={} gastos={}",
                        s.getId(), request.getPlaca(), totalIngresos, totalGastos);
            }
        }

        log.info("[LiquidacionService] liquidacion completada driverId={} sesiones={}", driverId, sessionsCerradas.size());
        return Map.of("liquidado", true, "sesiones", sessionsCerradas.size(), "sessionId", sessionId != null ? sessionId.toString() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularProducidoYango(String driverId, LocalDateTime desde, LocalDateTime hasta) {
        if (driverId == null || desde == null || hasta == null) return BigDecimal.ZERO;
        try {
            String dateFrom = desde.format(ISO_DATETIME_FORMATTER) + "-05:00";
            String dateTo = hasta.format(ISO_DATETIME_FORMATTER) + "-05:00";
            DriverOrdersResponse yango = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
            if (yango != null && yango.getOrders() != null && !yango.getOrders().isEmpty()) {
                return procesarDatosYango(yango.getOrders()).montoTotalProducido();
            }
        } catch (Exception e) {
            log.warn("[LiquidacionService] error calculando producido Yango driverId={} desde={} hasta={}: {}",
                    driverId, desde, hasta, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, Object> backfillProducido() {
        List<DriverClose> pendientes = driverCloseRepository.findByMontoTotalProducidoIsNull();
        int procesados = 0;
        int actualizados = 0;
        int fallidos = 0;
        log.info("[LiquidacionService] backfill producido: {} cierres por procesar", pendientes.size());

        for (DriverClose cierre : pendientes) {
            procesados++;
            try {
                BigDecimal producido = BigDecimal.ZERO;
                if (cierre.getShiftSessionId() != null) {
                    ShiftSession sesion = shiftSessionRepository.findById(cierre.getShiftSessionId()).orElse(null);
                    if (sesion != null && sesion.getStartedAt() != null) {
                        LocalDateTime fin = sesion.getClosedAt() != null ? sesion.getClosedAt() : sesion.getStartedAt().plusDays(1);
                        producido = calcularProducidoYango(sesion.getDriverId(), sesion.getStartedAt(), fin);
                    }
                }
                if (producido.compareTo(BigDecimal.ZERO) <= 0 && cierre.getFecha() != null) {
                    producido = calcularProducidoYango(cierre.getDriverId(),
                            cierre.getFecha().atStartOfDay(), cierre.getFecha().atTime(23, 59, 59));
                }
                if (producido.compareTo(BigDecimal.ZERO) > 0) {
                    cierre.setMontoTotalProducido(producido);
                    driverCloseRepository.save(cierre);
                    actualizados++;
                }
            } catch (Exception e) {
                fallidos++;
                log.warn("[LiquidacionService] backfill producido falló cierreId={}: {}", cierre.getId(), e.getMessage());
            }
        }

        log.info("[LiquidacionService] backfill producido completado procesados={} actualizados={} fallidos={}",
                procesados, actualizados, fallidos);
        return Map.of("procesados", procesados, "actualizados", actualizados, "fallidos", fallidos);
    }

    private LocalDateTime parseDateTimeLiquidar(String value) {        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDateTime.parse(value, ISO_DATETIME_FORMATTER);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private DatosYango procesarDatosYango(List<OrderInfoResponse> orders) {
        BigDecimal montoTotal = BigDecimal.ZERO;
        BigDecimal comisionApp = BigDecimal.ZERO;
        BigDecimal bonoYango = BigDecimal.ZERO;
        BigDecimal km = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        int totalViajes = 0;

        for (OrderInfoResponse o : orders) {
            if (o.getPrice() != null) montoTotal = montoTotal.add(BigDecimal.valueOf(o.getPrice()));
            if (o.getPriceCommissionService() != null) comisionApp = comisionApp.add(BigDecimal.valueOf(o.getPriceCommissionService()));
            if (o.getPriceCommissionPark() != null) comisionApp = comisionApp.add(BigDecimal.valueOf(o.getPriceCommissionPark()));
            if (o.getPriceBonus() != null) bonoYango = bonoYango.add(BigDecimal.valueOf(o.getPriceBonus()));
            if (o.getDistance() != null) km = km.add(BigDecimal.valueOf(o.getDistance()));
            if (o.getCash() != null) efectivo = efectivo.add(BigDecimal.valueOf(o.getCash()));
            totalViajes++;
        }

        comisionApp = comisionApp.abs();

        return new DatosYango(montoTotal, comisionApp, bonoYango, km, totalViajes, efectivo);
    }

    private BigDecimal calcularBono(int totalViajes) {
        List<BonusThreshold> thresholds = bonusThresholdRepository.findApplicableForDate(LocalDate.now());
        for (BonusThreshold bt : thresholds) {
            if (totalViajes >= bt.getMinTrips()) {
                return bt.getBonusAmount();
            }
        }
        return BigDecimal.ZERO;
    }

    private Double calcularPorcentajePago(int totalViajes) {
        List<PaymentPercentage> percentages = paymentPercentageRepository.findApplicableForDate(LocalDate.now());
        for (PaymentPercentage pp : percentages) {
            if (totalViajes >= pp.getMinValidatedTrips()) {
                return pp.getPercentage();
            }
        }
        return 0.2;
    }

    private BigDecimal calcularHorasEfectivas(List<ShiftSession> sessions) {
        long totalMinutos = 0;
        for (ShiftSession s : sessions) {
            if (s.getStartedAt() != null && s.getClosedAt() != null) {
                totalMinutos += Duration.between(s.getStartedAt(), s.getClosedAt()).toMinutes();
            }
        }
        return BigDecimal.valueOf(totalMinutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private LiquidacionSemanalResponse buildEmptySemanal(String driverId, LocalDate start, LocalDate end) {
        List<DiaLiquidacionInfo> dias = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dias.add(DiaLiquidacionInfo.builder()
                    .fecha(d).diaSemana(d.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "PE")))
                    .viajes(0).ingresos(BigDecimal.ZERO).ingresosPendientes(BigDecimal.ZERO).ingresosLiquidados(BigDecimal.ZERO)
                    .km(BigDecimal.ZERO).sesiones(0).estado("Sin actividad").sesionesDetalle(List.of())
                    .build());
        }
        return LiquidacionSemanalResponse.builder()
                .driverId(driverId).semanaInicio(start).semanaFin(end)
                .primerViaje(null).ultimoViaje(null)
                .totalSesiones(0).totalViajes(0).totalIngresos(BigDecimal.ZERO).totalKm(BigDecimal.ZERO)
                .dias(dias).sesionesPendientes(List.of())
                .tieneSesionesCerradas(false).tieneSesionActiva(false)
                .montoTotalProducido(BigDecimal.ZERO).bonoYango(BigDecimal.ZERO)
                .comisionApp(BigDecimal.ZERO).montoNeto(BigDecimal.ZERO)
                .produccionBonificable(BigDecimal.ZERO).bonoAdicViajes(BigDecimal.ZERO).bono(BigDecimal.ZERO)
                .porcentajePago(0.0).pago(BigDecimal.ZERO).pagoTotal(BigDecimal.ZERO)
                .utilidad(BigDecimal.ZERO).utilidadPorViaje(BigDecimal.ZERO).pagoPorViaje(BigDecimal.ZERO)
                .kmRecorrido(BigDecimal.ZERO).viajesPorHora(BigDecimal.ZERO).sesionesDetalle(List.of()).semanaCerrada(false)
                .gastoCombustible(BigDecimal.ZERO)
                .build();
    }

    private LiquidacionPendienteResponse buildEmptyPendiente(String driverId, LocalDateTime desde, LocalDateTime hasta, boolean esPrimera) {
        return LiquidacionPendienteResponse.builder()
                .driverId(driverId)
                .periodoDesde(desde)
                .periodoHasta(hasta)
                .esPrimeraLiquidacion(esPrimera)
                .totalSesiones(0)
                .totalViajes(0)
                .viajesPorHora(BigDecimal.ZERO)
                .kmRecorrido(BigDecimal.ZERO)
                .montoTotalProducido(BigDecimal.ZERO)
                .placa("")
                .carBrandModel("")
                .semanaCerrada(false)
                .bonoYango(BigDecimal.ZERO)
                .comisionApp(BigDecimal.ZERO)
                .montoNeto(BigDecimal.ZERO)
                .produccionBonificable(BigDecimal.ZERO)
                .bono(BigDecimal.ZERO)
                .porcentajePago(0.0)
                .pago(BigDecimal.ZERO)
                .pagoTotal(BigDecimal.ZERO)
                .utilidad(BigDecimal.ZERO)
                .utilidadPorViaje(BigDecimal.ZERO)
                .pagoPorViaje(BigDecimal.ZERO)
                .diasTrabajados(0)
                .sesionesPendientes(List.of())
                .dias(List.of())
                .efectivo(BigDecimal.ZERO)
                .build();
    }

    private record DatosYango(BigDecimal montoTotalProducido, BigDecimal comisionApp, BigDecimal bonoYango, BigDecimal km, int totalViajes, BigDecimal efectivo) {}

    @Override
    public void limpiarFacturacion(String driverId, LocalDate desde, LocalDate hasta) {
        facturacionSemanalRepository.deleteOverlappingWithDriver(driverId, desde, hasta);
        log.info("[LiquidacionService] facturación limpiada driverId={} desde={} hasta={}", driverId, desde, hasta);
    }
}
