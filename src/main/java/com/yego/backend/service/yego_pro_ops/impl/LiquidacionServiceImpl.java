package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse.DiaLiquidacionInfo;
import com.yego.backend.entity.yego_pro_ops.api.response.LiquidacionSemanalResponse.SesionDiaInfo;
import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import com.yego.backend.entity.yego_pro_ops.entities.Trip;
import com.yego.backend.repository.yego_pro_ops.ShiftSessionRepository;
import com.yego.backend.repository.yego_pro_ops.TripRepository;
import com.yego.backend.service.yego_pro_ops.LiquidacionService;
import com.yego.backend.service.yego_pro_ops.ShiftSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacionServiceImpl implements LiquidacionService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ShiftSessionRepository shiftSessionRepository;
    private final TripRepository tripRepository;
    private final ShiftSessionService shiftSessionService;

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
            return buildEmptyResponse(driverId, weekStart, weekEnd);
        }

        List<UUID> sessionIds = sessions.stream().map(ShiftSession::getId).collect(Collectors.toList());
        List<Trip> allTrips = new ArrayList<>();
        for (UUID sid : sessionIds) {
            allTrips.addAll(tripRepository.findByShiftSessionId(sid));
        }

        Map<LocalDate, List<ShiftSession>> sessionsByDay = new LinkedHashMap<>();
        for (ShiftSession s : sessions) {
            LocalDate dia = s.getStartedAt().toLocalDate();
            if (!dia.isBefore(weekStart) && !dia.isAfter(weekEnd)) {
                sessionsByDay.computeIfAbsent(dia, k -> new ArrayList<>()).add(s);
            }
        }

        boolean tieneActiva = sessions.stream().anyMatch(s -> "active".equals(s.getStatus()));
        boolean tieneCerrada = sessions.stream().anyMatch(s -> "closed".equals(s.getStatus()));

        BigDecimal totalPendiente = sessions.stream()
                .filter(s -> "closed".equals(s.getStatus()))
                .map(s -> tripRepository.findByShiftSessionId(s.getId()).stream()
                        .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DiaLiquidacionInfo> dias = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        int totalViajes = 0;
        BigDecimal totalKm = BigDecimal.ZERO;

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

                if ("closed".equals(s.getStatus())) {
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
                boolean hayCerrada = sesionesDia.stream().anyMatch(s -> "closed".equals(s.getStatus()));
                boolean haySettled = sesionesDia.stream().anyMatch(s -> "settled".equals(s.getStatus()));
                boolean todasSettled = sesionesDia.stream().allMatch(s -> "settled".equals(s.getStatus()));

                if (todasSettled) {
                    estado = "Liquidado";
                } else if (hayActiva) {
                    estado = "En curso";
                } else if (hayCerrada && haySettled) {
                    estado = "Pendiente parcial";
                } else if (hayCerrada) {
                    estado = "Cerrado";
                } else if (haySettled) {
                    estado = "Liquidado";
                } else {
                    estado = "Sin actividad";
                }
            }

            totalViajes += viajesDia;
            totalKm = totalKm.add(kmDia);

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
                .filter(s -> "closed".equals(s.getStatus()))
                .map(ShiftSession::getId)
                .collect(Collectors.toList());

        return LiquidacionSemanalResponse.builder()
                .driverId(driverId)
                .semanaInicio(weekStart)
                .semanaFin(weekEnd)
                .totalSesiones(sessions.size())
                .totalViajes(totalViajes)
                .totalIngresos(totalPendiente)
                .totalKm(totalKm)
                .primerViaje(primerViaje)
                .ultimoViaje(ultimoViaje)
                .dias(dias)
                .sesionesPendientes(sesionesPendientes)
                .tieneSesionesCerradas(tieneCerrada)
                .tieneSesionActiva(tieneActiva)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> liquidarSemana(String driverId) {
        List<ShiftSession> sessions = shiftSessionRepository.findByDriverIdOrderByStartedAtDesc(driverId).stream()
                .filter(s -> "closed".equals(s.getStatus()))
                .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            return Map.of("liquidado", false, "mensaje", "No hay sesiones cerradas para liquidar");
        }

        int count = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (ShiftSession s : sessions) {
            List<Trip> trips = tripRepository.findByShiftSessionId(s.getId());
            for (Trip t : trips) {
                total = total.add(t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO);
            }
            try {
                shiftSessionService.settleSession(s.getId(), null);
                count++;
            } catch (Exception e) {
                log.error("[LiquidacionService] error liquidando sesion {}: {}", s.getId(), e.getMessage());
            }
        }

        log.info("[LiquidacionService] liquidacion completada driverId={} sesiones={} total={}", driverId, count, total);
        return Map.of("liquidado", true, "sesiones", count, "total", total);
    }

    private LiquidacionSemanalResponse buildEmptyResponse(String driverId, LocalDate start, LocalDate end) {
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
                .totalSesiones(0).totalViajes(0).totalIngresos(BigDecimal.ZERO).totalKm(BigDecimal.ZERO)
                .dias(dias).sesionesPendientes(List.of())
                .tieneSesionesCerradas(false).tieneSesionActiva(false)
                .build();
    }
}
