package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.BillingConfigResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ResumenSemanalResponse;
import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import com.yego.backend.repository.yego_pro_ops.BonusThresholdRepository;
import com.yego.backend.repository.yego_pro_ops.CalculatedShiftRepository;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.FacturacionSemanalRepository;
import com.yego.backend.repository.yego_pro_ops.PaymentPercentageRepository;
import com.yego.backend.entity.yego_api_externo.api.response.YangoIncomeSummary;
import com.yego.backend.service.yego_api_externo.YangoWeeklyService;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CalculatedShiftServiceImpl implements CalculatedShiftService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final int HORA_CORTE_SUBTURNO = 12;
    private static final int LOG_PROGRESO_CADA_N = 5;
    private static final long DETALLE_CACHE_TTL_MS = 60_000L;
    private static final long CONDUCTORES_DIR_CACHE_TTL_MS = 120_000L;
    private static final long RESUMEN_SEMANAL_CACHE_TTL_MS = 90_000L;

    private static final ConcurrentHashMap<String, CachedResponse<ResumenSemanalResponse>> resumenSemanalCache = new ConcurrentHashMap<>();

    private static final int SHIFT_EXECUTOR_THREADS = 8;
    private static final int BATCH_PARALELISMO = 4;
    private static final long IN_FLIGHT_RELEASE_DELAY_MS = 5_000L;

    private static final ConcurrentHashMap<String, CachedResponse<DriverPaymentSummaryResponse>> resumenPagosCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CachedResponse<PaidShiftsResponse>> turnosPagadosCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, CompletableFuture<List<CalculatedShift>>> calculosEnCurso = new ConcurrentHashMap<>();

    private final Object conductoresCacheLock = new Object();
    private volatile CachedResponse<Map<String, ConductorInfo>> directorioConductoresCache;

    private final CalculatedShiftRepository calculatedShiftRepository;
    private final DriverCloseRepository driverCloseRepository;
    private final FacturacionSemanalRepository facturacionSemanalRepository;
    private final BonusThresholdRepository bonusThresholdRepository;
    private final PaymentPercentageRepository paymentPercentageRepository;
    private final DriverOrdersService driverOrdersService;
    private final FleetDriverService fleetDriverService;
    private final YangoWeeklyService yangoWeeklyService;
    private final String parkId;
    private final ExecutorService shiftExecutor;

    public CalculatedShiftServiceImpl(
            CalculatedShiftRepository calculatedShiftRepository,
            DriverCloseRepository driverCloseRepository,
            FacturacionSemanalRepository facturacionSemanalRepository,
            BonusThresholdRepository bonusThresholdRepository,
            PaymentPercentageRepository paymentPercentageRepository,
            @Lazy DriverOrdersService driverOrdersService,
            FleetDriverService fleetDriverService,
            YangoWeeklyService yangoWeeklyService,
            @Value("${yego.yango.park-id:64085dd85e124e2c808806f70d527ea8}") String parkId) {
        this.calculatedShiftRepository = calculatedShiftRepository;
        this.driverCloseRepository = driverCloseRepository;
        this.facturacionSemanalRepository = facturacionSemanalRepository;
        this.bonusThresholdRepository = bonusThresholdRepository;
        this.paymentPercentageRepository = paymentPercentageRepository;
        this.driverOrdersService = driverOrdersService;
        this.fleetDriverService = fleetDriverService;
        this.yangoWeeklyService = yangoWeeklyService;
        this.parkId = (parkId != null && !parkId.isBlank()) ? parkId.trim() : "64085dd85e124e2c808806f70d527ea8";
        this.shiftExecutor = Executors.newFixedThreadPool(SHIFT_EXECUTOR_THREADS, namedFactory("shift-worker"));
    }

    @PreDestroy
    public void shutdown() {
        shiftExecutor.shutdown();
        try {
            if (!shiftExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                shiftExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            shiftExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }

    private void calcularYGuardarHorasTurno(String driverId, LocalDate fecha) {
        if (!validarParametros(driverId, fecha)) return;

        try {
            List<OrderInfoResponse> viajesDelDia = obtenerViajesDelDia(driverId, fecha);
            Map<CalculatedShift.TipoTurno, List<OrderInfoResponse>> porSubturno = agruparPorSubturno(viajesDelDia);

            guardarSubturno(driverId, fecha, CalculatedShift.TipoTurno.manana,
                porSubturno.getOrDefault(CalculatedShift.TipoTurno.manana, List.of()));
            guardarSubturno(driverId, fecha, CalculatedShift.TipoTurno.tarde,
                porSubturno.getOrDefault(CalculatedShift.TipoTurno.tarde, List.of()));
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error subturnos diarios driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage(), e);
        }
    }

    @Override
    public void procesarHorasTurnoDiaAnterior() {
        LocalDate fechaAnterior = LocalDate.now(LIMA_ZONE).minusDays(1);
        log.info("[CalculatedShiftService] inicio batch subturnos día anterior fecha={} paralelismo={}",
            fechaAnterior, BATCH_PARALELISMO);

        try {
            DriverListResponse listaConductores = fleetDriverService.obtenerListaConductores();
            if (listaConductores == null || listaConductores.getContractors() == null
                    || listaConductores.getContractors().isEmpty()) {
                log.warn("[CalculatedShiftService] sin conductores para procesar");
                return;
            }
            EstadisticasProcesamiento stats = procesarConductoresEnParalelo(listaConductores.getContractors(), fechaAnterior);
            log.info("[CalculatedShiftService] fin batch fecha={} ok={} omitidos={} errores={} duracion={}s",
                fechaAnterior, stats.totalProcesados(), stats.totalOmitidos(), stats.totalErrores(), stats.tiempoTotal() / 1000);
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error crítico batch fecha={}: {}", fechaAnterior, e.getMessage(), e);
        }
    }

    @Override
    public FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId) {
        try {
            List<CalculatedShift> shifts = calculatedShiftRepository.findByDriverIdOrderByFecha(driverId);
            if (shifts.isEmpty()) {
                return FechasConTiposTurnoResponse.builder()
                    .driverId(driverId).fechas(new ArrayList<>()).build();
            }
            return FechasConTiposTurnoResponse.builder()
                .driverId(driverId)
                .fechas(agruparShiftsPorFecha(shifts))
                .build();
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error fechas-turnos driverId={}: {}", driverId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener fechas con subturnos: " + e.getMessage(), e);
        }
    }

    @Override
    public DriverPaymentSummaryResponse obtenerResumenPagos(String fecha) {
        CachedResponse<DriverPaymentSummaryResponse> cached = resumenPagosCache.get(fecha);
        if (cached != null && !cached.isExpired()) return cached.response();

        try {
            LocalDate fechaLocal = parsearFecha(fecha);
            if (fechaLocal == null) {
                throw new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD");
            }
            List<CalculatedShift> turnosPorFecha = calculatedShiftRepository.findByFechaOrderByDriverId(fechaLocal);
            if (turnosPorFecha.isEmpty()) {
                DriverPaymentSummaryResponse vacia = DriverPaymentSummaryResponse.builder()
                    .conductores(new ArrayList<>()).build();
                resumenPagosCache.put(fecha, CachedResponse.of(vacia));
                return vacia;
            }
            Map<String, List<CalculatedShift>> turnosPorConductor = turnosPorFecha.stream()
                .collect(Collectors.groupingBy(CalculatedShift::getDriverId));
            Map<String, ConductorInfo> infoConductores = obtenerInfoConductores();

            DriverPaymentSummaryResponse response = DriverPaymentSummaryResponse.builder()
                .conductores(construirInfoConductores(turnosPorConductor, infoConductores, fechaLocal))
                .build();
            resumenPagosCache.put(fecha, CachedResponse.of(response));
            return response;
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error resumen pagos fecha={}: {}", fecha, e.getMessage(), e);
            throw new RuntimeException("Error al obtener resumen de pagos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CalculatedShift> obtenerOCalcularTurnos(String driverId, String fecha) {
        LocalDate fechaLocal = parsearFecha(fecha);
        if (fechaLocal == null) {
            throw new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD");
        }
        List<CalculatedShift> existentes = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fechaLocal);
        if (!existentes.isEmpty()) return existentes;
        try {
            return calcularTurnosAsync(driverId, fechaLocal).join();
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error obtenerOCalcular driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage(), e);
            throw new RuntimeException("Error al obtener o calcular turnos: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<List<CalculatedShift>> calcularTurnosAsync(String driverId, String fecha) {
        LocalDate fechaLocal = parsearFecha(fecha);
        if (fechaLocal == null) {
            CompletableFuture<List<CalculatedShift>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD"));
            return failed;
        }
        return calcularTurnosAsync(driverId, fechaLocal);
    }

    @Override
    public CompletableFuture<List<CalculatedShift>> recalcularTurnos(String driverId, String fecha) {
        LocalDate fechaLocal = parsearFecha(fecha);
        if (fechaLocal == null) {
            CompletableFuture<List<CalculatedShift>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD"));
            return failed;
        }
        log.info("[CalculatedShiftService] recalcular turnos driverId={} fecha={}", driverId, fecha);

        int cierresEliminados = driverCloseRepository.deleteByDriverIdAndFecha(driverId, fechaLocal);
        int turnosEliminados = calculatedShiftRepository.deleteByDriverIdAndFecha(driverId, fechaLocal);
        log.info("[CalculatedShiftService] eliminados driverId={} fecha={} -> cierres={} turnos={}",
            driverId, fecha, cierresEliminados, turnosEliminados);

        invalidarCacheDetalle(fecha);

        calculosEnCurso.remove(driverId + "::" + fechaLocal);

        return calcularTurnosAsync(driverId, fechaLocal);
    }

    private CompletableFuture<List<CalculatedShift>> calcularTurnosAsync(String driverId, LocalDate fecha) {
        String key = driverId + "::" + fecha;
        return calculosEnCurso.computeIfAbsent(key, k -> {
            CompletableFuture<List<CalculatedShift>> future = CompletableFuture.supplyAsync(
                () -> ejecutarCalculoTurnos(driverId, fecha), shiftExecutor);
            future.whenComplete((res, err) ->
                CompletableFuture.delayedExecutor(IN_FLIGHT_RELEASE_DELAY_MS, TimeUnit.MILLISECONDS, shiftExecutor)
                    .execute(() -> calculosEnCurso.remove(key)));
            return future;
        });
    }

    private List<CalculatedShift> ejecutarCalculoTurnos(String driverId, LocalDate fecha) {
        List<CalculatedShift> yaExistentes = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fecha);
        if (!yaExistentes.isEmpty()) return yaExistentes;

        calcularYGuardarHorasTurno(driverId, fecha);
        calculatedShiftRepository.markAsManualByDriverIdAndFecha(driverId, fecha);
        List<CalculatedShift> turnos = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fecha);
        return turnos.isEmpty() ? new ArrayList<>() : turnos;
    }

    @Override
    public PaidShiftsResponse obtenerTurnosPagados(String fecha) {
        String cacheKey = fecha != null ? fecha : "__all__";
        CachedResponse<PaidShiftsResponse> cached = turnosPagadosCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.response();

        try {
            LocalDate fechaLocal = fecha != null ? parsearFecha(fecha) : null;
            if (fecha != null && fechaLocal == null) {
                throw new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD");
            }
            List<CalculatedShift> turnosPagados = fechaLocal != null
                ? calculatedShiftRepository.findByPagadoTrueAndFecha(fechaLocal)
                : calculatedShiftRepository.findByPagadoTrue();

            if (turnosPagados.isEmpty()) {
                PaidShiftsResponse vacia = PaidShiftsResponse.builder()
                    .totalConductores(0).conductores(new ArrayList<>()).build();
                turnosPagadosCache.put(cacheKey, CachedResponse.of(vacia));
                return vacia;
            }
            Map<String, List<CalculatedShift>> turnosPorConductor = turnosPagados.stream()
                .collect(Collectors.groupingBy(CalculatedShift::getDriverId));
            Map<String, ConductorInfo> infoConductores = obtenerInfoConductores();
            List<PaidShiftsResponse.ConductorTurnosPagadosInfo> conductoresInfo =
                construirInfoConductoresTurnosPagados(turnosPorConductor, infoConductores);

            PaidShiftsResponse response = PaidShiftsResponse.builder()
                .totalConductores(conductoresInfo.size())
                .conductores(conductoresInfo)
                .build();
            turnosPagadosCache.put(cacheKey, CachedResponse.of(response));
            return response;
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error turnos pagados fecha={}: {}", fecha, e.getMessage(), e);
            throw new RuntimeException("Error al obtener turnos pagados: " + e.getMessage(), e);
        }
    }

    @Override
    public void invalidarCacheDetalle(String fecha) {
        if (fecha == null || fecha.isEmpty()) return;
        resumenPagosCache.remove(fecha);
        turnosPagadosCache.remove(fecha);
        directorioConductoresCache = null;
        resumenSemanalCache.clear();
    }

    private List<OrderInfoResponse> obtenerViajesDelDia(String driverId, LocalDate fecha) {
        ZonedDateTime inicio = fecha.atStartOfDay(LIMA_ZONE);
        ZonedDateTime fin = fecha.atTime(23, 59, 59).atZone(LIMA_ZONE);

        DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(
            driverId, inicio.format(API_DATE_FORMATTER), fin.format(API_DATE_FORMATTER));

        if (respuesta == null || respuesta.getOrders() == null || respuesta.getOrders().isEmpty()) {
            return List.of();
        }
        return respuesta.getOrders().stream()
            .filter(v -> {
                LocalDateTime bookedAt = obtenerFechaInicioViaje(v);
                return bookedAt != null && bookedAt.toLocalDate().equals(fecha);
            })
            .collect(Collectors.toList());
    }

    private Map<CalculatedShift.TipoTurno, List<OrderInfoResponse>> agruparPorSubturno(List<OrderInfoResponse> viajes) {
        Map<CalculatedShift.TipoTurno, List<OrderInfoResponse>> resultado = new EnumMap<>(CalculatedShift.TipoTurno.class);
        resultado.put(CalculatedShift.TipoTurno.manana, new ArrayList<>());
        resultado.put(CalculatedShift.TipoTurno.tarde, new ArrayList<>());

        for (OrderInfoResponse viaje : viajes) {
            LocalDateTime bookedAt = obtenerFechaInicioViaje(viaje);
            if (bookedAt == null) continue;
            CalculatedShift.TipoTurno subturno = bookedAt.getHour() < HORA_CORTE_SUBTURNO
                ? CalculatedShift.TipoTurno.manana
                : CalculatedShift.TipoTurno.tarde;
            resultado.get(subturno).add(viaje);
        }
        return resultado;
    }

    private void guardarSubturno(String driverId, LocalDate fecha, CalculatedShift.TipoTurno tipoTurno,
                                  List<OrderInfoResponse> viajes) {
        Optional<CalculatedShift> existenteOpt = calculatedShiftRepository
            .findByDriverIdAndFechaAndTipoTurno(driverId, fecha, tipoTurno);

        if (viajes.isEmpty()) {
            existenteOpt.filter(t -> Boolean.FALSE.equals(t.getPagado()) && Boolean.FALSE.equals(t.getEsManual()))
                .ifPresent(calculatedShiftRepository::delete);
            return;
        }

        LocalDateTime horaInicio = obtenerPrimerInicio(viajes);
        LocalDateTime horaFin = obtenerUltimoFin(viajes);
        if (horaInicio == null) {
            log.warn("[CalculatedShiftService] subturno {} sin hora_inicio driverId={} fecha={}",
                tipoTurno, driverId, fecha);
            return;
        }
        if (horaFin == null || horaFin.isBefore(horaInicio)) horaFin = horaInicio;

        CalculatedShift turno = existenteOpt.orElseGet(() -> {
            CalculatedShift nuevo = new CalculatedShift();
            nuevo.setDriverId(driverId);
            nuevo.setFecha(fecha);
            nuevo.setPagado(false);
            nuevo.setEsManual(false);
            return nuevo;
        });

        turno.setDriverId(driverId);
        turno.setFecha(fecha);
        turno.setTipoTurno(tipoTurno);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFin(horaFin);
        turno.setDuracionMinutos((int) Duration.between(horaInicio, horaFin).toMinutes());
        turno.setCantidadViajes(viajes.size());
        turno.setMontoTotal(calcularMontoLiquidacion(viajes));
        turno.setProduccionTotal(calcularProduccionTotal(viajes));
        turno.setComisionesServicio(calcularComisionesServicio(viajes));
        turno.setEstado(CalculatedShift.EstadoTurno.finalizado);

        if (turno.getPlaca() == null || turno.getPlaca().isEmpty()) {
            turno.setPlaca(obtenerPlacaConductor(driverId, fecha));
        }

        calculatedShiftRepository.save(turno);
    }

    private LocalDateTime obtenerFechaInicioViaje(OrderInfoResponse viaje) {
        if (viaje == null || viaje.getBookedAt() == null) return null;
        try {
            return parsearFechaViaje(viaje.getBookedAt()).withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime obtenerFechaFinViaje(OrderInfoResponse viaje) {
        if (viaje == null) return null;
        if (viaje.getEndedAt() != null && !viaje.getEndedAt().isEmpty()) {
            try {
                return parsearFechaViaje(viaje.getEndedAt()).withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
            } catch (Exception ignored) {}
        }
        return obtenerFechaInicioViaje(viaje);
    }

    private LocalDateTime obtenerPrimerInicio(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .map(this::obtenerFechaInicioViaje)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }

    private LocalDateTime obtenerUltimoFin(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .map(this::obtenerFechaFinViaje)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private ZonedDateTime parsearFechaViaje(String fechaStr) {
        try {
            return ZonedDateTime.parse(fechaStr);
        } catch (Exception ignored) {}
        try {
            return LocalDateTime.parse(fechaStr, ORDER_DATE_FORMATTER).atZone(LIMA_ZONE);
        } catch (Exception ignored) {}
        try {
            return LocalDateTime.parse(fechaStr, DATE_FORMATTER_WITH_MILLIS).atZone(LIMA_ZONE);
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error parseando fecha: {}", fechaStr);
            throw new RuntimeException("Error parseando fecha: " + fechaStr, e);
        }
    }

    private Double calcularMontoLiquidacion(List<OrderInfoResponse> viajes) {
        if (viajes == null || viajes.isEmpty()) return 0.0;
        double total = 0.0;
        for (OrderInfoResponse v : viajes) {
            if (v.getCash() != null) total += v.getCash();
        }
        return round2(total);
    }

    private Double calcularProduccionTotal(List<OrderInfoResponse> viajes) {
        if (viajes == null || viajes.isEmpty()) return 0.0;
        double total = 0.0;
        for (OrderInfoResponse v : viajes) {
            if (v.getCash() != null) total += v.getCash();
            if (v.getCard() != null) total += v.getCard();
            if (v.getPriceCorporate() != null) total += v.getPriceCorporate();
            if (v.getPriceTip() != null) total += v.getPriceTip();
            if (v.getPricePromotion() != null) total += v.getPricePromotion();
            if (v.getPriceBonus() != null) total += v.getPriceBonus();
        }
        return round2(total);
    }

    private Double calcularComisionesServicio(List<OrderInfoResponse> viajes) {
        if (viajes == null || viajes.isEmpty()) return 0.0;
        double total = 0.0;
        for (OrderInfoResponse v : viajes) {
            if (v.getPriceCommissionService() != null) total += v.getPriceCommissionService();
        }
        return round2(total);
    }

    private Double sumarProduccionTotal(List<CalculatedShift> turnos) {
        return round2(turnos.stream()
            .mapToDouble(t -> t.getProduccionTotal() != null ? t.getProduccionTotal() : 0.0)
            .sum());
    }

    private Double sumarComisionesServicio(List<CalculatedShift> turnos) {
        return round2(turnos.stream()
            .mapToDouble(t -> t.getComisionesServicio() != null ? t.getComisionesServicio() : 0.0)
            .sum());
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private EstadisticasProcesamiento procesarConductoresEnParalelo(
            List<DriverListResponse.ContractorResponse> conductores, LocalDate fecha) {
        long inicio = System.currentTimeMillis();
        int totalConductores = conductores.size();
        AtomicInteger procesados = new AtomicInteger();
        AtomicInteger omitidos = new AtomicInteger();
        AtomicInteger errores = new AtomicInteger();
        Semaphore limite = new Semaphore(BATCH_PARALELISMO);

        CompletableFuture<?>[] tareas = conductores.stream()
            .filter(c -> c.getId() != null && !c.getId().isEmpty())
            .map(c -> CompletableFuture.runAsync(() -> {
                try {
                    limite.acquire();
                    procesarConductorBatch(c.getId(), fecha, totalConductores, procesados, omitidos, errores);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errores.incrementAndGet();
                } finally {
                    limite.release();
                }
            }, shiftExecutor))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(tareas).join();
        return new EstadisticasProcesamiento(
            procesados.get(), omitidos.get(), errores.get(), System.currentTimeMillis() - inicio);
    }

    private void procesarConductorBatch(String driverId, LocalDate fecha, int total,
                                         AtomicInteger procesados, AtomicInteger omitidos, AtomicInteger errores) {
        try {
            if (tieneTurnoManual(driverId, fecha) || tieneTurnosPagados(driverId, fecha)) {
                omitidos.incrementAndGet();
                return;
            }
            calcularYGuardarHorasTurno(driverId, fecha);
            int hechos = procesados.incrementAndGet();
            if (hechos % LOG_PROGRESO_CADA_N == 0) {
                log.info("[CalculatedShiftService] progreso {}/{} (omitidos={} errores={})",
                    hechos, total, omitidos.get(), errores.get());
            }
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage(), e);
            errores.incrementAndGet();
        }
    }

    private boolean tieneTurnoManual(String driverId, LocalDate fecha) {
        return !calculatedShiftRepository.findByDriverIdAndFechaAndEsManual(driverId, fecha).isEmpty();
    }

    private boolean tieneTurnosPagados(String driverId, LocalDate fecha) {
        List<CalculatedShift> turnos = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fecha);
        if (turnos.isEmpty()) return false;
        return turnos.stream().allMatch(t -> Boolean.TRUE.equals(t.getPagado()));
    }

    private List<FechasConTiposTurnoResponse.FechaConTiposTurno> agruparShiftsPorFecha(List<CalculatedShift> shifts) {
        Map<LocalDate, List<CalculatedShift>> shiftsPorFecha = shifts.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getFecha));

        return shiftsPorFecha.entrySet().stream()
            .map(entry -> FechasConTiposTurnoResponse.FechaConTiposTurno.builder()
                .fecha(entry.getKey())
                .tiposTurno(entry.getValue().stream()
                    .map(s -> FechasConTiposTurnoResponse.TipoTurnoInfo.builder()
                        .id(s.getId())
                        .tipoTurno(s.getTipoTurno().name())
                        .build())
                    .collect(Collectors.toList()))
                .build())
            .sorted(Comparator.comparing(FechasConTiposTurnoResponse.FechaConTiposTurno::getFecha))
            .collect(Collectors.toList());
    }

    private Map<String, ConductorInfo> obtenerInfoConductores() {
        CachedResponse<Map<String, ConductorInfo>> snap = directorioConductoresCache;
        if (snap != null && !snap.isExpired()) return snap.response();

        synchronized (conductoresCacheLock) {
            snap = directorioConductoresCache;
            if (snap != null && !snap.isExpired()) return snap.response();

            Map<String, ConductorInfo> map = construirDirectorioConductoresDesdeYango();
            directorioConductoresCache = CachedResponse.of(Map.copyOf(map), CONDUCTORES_DIR_CACHE_TTL_MS);
            return directorioConductoresCache.response();
        }
    }

    private Map<String, ConductorInfo> construirDirectorioConductoresDesdeYango() {
        DriverListResponse listaConductores = fleetDriverService.obtenerListaConductores();
        Map<String, ConductorInfo> infoMap = new HashMap<>();
        if (listaConductores == null || listaConductores.getContractors() == null) return infoMap;

        for (DriverListResponse.ContractorResponse contractor : listaConductores.getContractors()) {
            if (contractor.getId() == null) continue;
            infoMap.put(contractor.getId(), new ConductorInfo(
                contractor.getAvatarUrl(), contractor.getFullName(), contractor.getPhone()));
        }
        return infoMap;
    }

    private List<DriverPaymentSummaryResponse.ConductorPaymentInfo> construirInfoConductores(
            Map<String, List<CalculatedShift>> turnosPorConductor,
            Map<String, ConductorInfo> infoConductores,
            LocalDate fecha) {
        return turnosPorConductor.entrySet().stream()
            .map(e -> construirInfoConductor(e.getKey(), e.getValue(), infoConductores, fecha))
            .collect(Collectors.toList());
    }

    private DriverPaymentSummaryResponse.ConductorPaymentInfo construirInfoConductor(
            String driverId, List<CalculatedShift> turnos,
            Map<String, ConductorInfo> infoConductores, LocalDate fecha) {

        ConductorInfo info = infoConductores.getOrDefault(driverId, ConductorInfo.EMPTY);
        Integer cantidadViajes = calcularCantidadViajesTotal(turnos);
        String placa = turnos.stream()
            .map(CalculatedShift::getPlaca)
            .filter(p -> p != null && !p.isEmpty())
            .findFirst()
            .orElseGet(() -> driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(driverId, fecha)
                .map(DriverClose::getPlaca)
                .filter(p -> p != null && !p.isEmpty())
                .orElseGet(() -> driverCloseRepository.findFirstByDriverIdOrderByIdDesc(driverId)
                    .map(DriverClose::getPlaca)
                    .filter(p -> p != null && !p.isEmpty())
                    .orElse(null)));

        return DriverPaymentSummaryResponse.ConductorPaymentInfo.builder()
            .driverId(driverId)
            .avatarUrl(info.avatarUrl())
            .nombre(info.nombre())
            .telefono(info.telefono())
            .placa(placa)
            .montoTotalPagar(calcularMontoTotalPagar(turnos))
            .produccionTotal(sumarProduccionTotal(turnos))
            .comisionesServicio(sumarComisionesServicio(turnos))
            .cantidadTurnos(turnos.size())
            .cantidadViajes(cantidadViajes)
            .viajesPorHora(calcularViajesPorHoraPromedio(turnos))
            .turnos(mapearTurnosAInfo(turnos))
            .build();
    }

    private Double calcularMontoTotalPagar(List<CalculatedShift> turnos) {
        return round2(turnos.stream()
            .filter(t -> t.getPagado() == null || !t.getPagado())
            .mapToDouble(t -> t.getMontoTotal() != null ? t.getMontoTotal() : 0.0)
            .sum());
    }

    private Double calcularViajesPorHora(Integer cantidadViajes, Integer duracionMinutos) {
        if (cantidadViajes == null || cantidadViajes == 0 || duracionMinutos == null || duracionMinutos == 0) {
            return 0.0;
        }
        return round2(cantidadViajes / (duracionMinutos / 60.0));
    }

    private Integer calcularCantidadViajesTotal(List<CalculatedShift> turnos) {
        return turnos.stream()
            .mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0)
            .sum();
    }

    private Double calcularViajesPorHoraPromedio(List<CalculatedShift> turnos) {
        Integer cantidadViajes = calcularCantidadViajesTotal(turnos);
        Integer duracionTotal = turnos.stream()
            .mapToInt(t -> t.getDuracionMinutos() != null ? t.getDuracionMinutos() : 0)
            .sum();
        return calcularViajesPorHora(cantidadViajes, duracionTotal);
    }

    private List<DriverPaymentSummaryResponse.TurnoInfo> mapearTurnosAInfo(List<CalculatedShift> turnos) {
        return turnos.stream().map(this::mapearTurnoAInfo).collect(Collectors.toList());
    }

    private DriverPaymentSummaryResponse.TurnoInfo mapearTurnoAInfo(CalculatedShift turno) {
        return DriverPaymentSummaryResponse.TurnoInfo.builder()
            .id(turno.getId())
            .fecha(turno.getFecha().toString())
            .horaInicio(turno.getHoraInicio() != null ? turno.getHoraInicio().toString() : null)
            .horaFin(turno.getHoraFin() != null ? turno.getHoraFin().toString() : null)
            .tipoTurno(turno.getTipoTurno() != null ? turno.getTipoTurno().name() : null)
            .duracionMinutos(turno.getDuracionMinutos())
            .montoTotal(turno.getMontoTotal())
            .pagado(Boolean.TRUE.equals(turno.getPagado()))
            .build();
    }

    private List<PaidShiftsResponse.ConductorTurnosPagadosInfo> construirInfoConductoresTurnosPagados(
            Map<String, List<CalculatedShift>> turnosPorConductor,
            Map<String, ConductorInfo> infoConductores) {
        return turnosPorConductor.entrySet().stream()
            .map(e -> construirInfoConductorTurnosPagados(e.getKey(), e.getValue(), infoConductores))
            .collect(Collectors.toList());
    }

    private PaidShiftsResponse.ConductorTurnosPagadosInfo construirInfoConductorTurnosPagados(
            String driverId, List<CalculatedShift> turnos, Map<String, ConductorInfo> infoConductores) {

        ConductorInfo info = infoConductores.getOrDefault(driverId, ConductorInfo.EMPTY);
        double montoTotalPagado = round2(turnos.stream()
            .mapToDouble(t -> t.getMontoTotal() != null ? t.getMontoTotal() : 0.0)
            .sum());

        return PaidShiftsResponse.ConductorTurnosPagadosInfo.builder()
            .driverId(driverId)
            .avatarUrl(info.avatarUrl())
            .nombre(info.nombre())
            .telefono(info.telefono())
            .cantidadTurnos(turnos.size())
            .cantidadViajes(calcularCantidadViajesTotal(turnos))
            .viajesPorHora(calcularViajesPorHoraPromedio(turnos))
            .montoTotalPagado(montoTotalPagado)
            .produccionTotal(sumarProduccionTotal(turnos))
            .comisionesServicio(sumarComisionesServicio(turnos))
            .turnos(turnos.stream().map(this::convertirATurnoPagadoInfo).collect(Collectors.toList()))
            .build();
    }

    private PaidShiftsResponse.TurnoPagadoInfo convertirATurnoPagadoInfo(CalculatedShift turno) {
        return PaidShiftsResponse.TurnoPagadoInfo.builder()
            .id(turno.getId())
            .fecha(turno.getFecha().toString())
            .horaInicio(turno.getHoraInicio() != null ? turno.getHoraInicio().toString() : null)
            .horaFin(turno.getHoraFin() != null ? turno.getHoraFin().toString() : null)
            .tipoTurno(turno.getTipoTurno() != null ? turno.getTipoTurno().name() : null)
            .duracionMinutos(turno.getDuracionMinutos())
            .montoTotal(turno.getMontoTotal())
            .pagado(Boolean.TRUE.equals(turno.getPagado()))
            .build();
    }

    private boolean validarParametros(String driverId, LocalDate fecha) {
        if (fecha == null) {
            log.error("[CalculatedShiftService] fecha null driverId={}", driverId);
            return false;
        }
        if (driverId == null || driverId.isEmpty()) {
            log.error("[CalculatedShiftService] driverId null/vacío");
            return false;
        }
        return true;
    }

    private String obtenerPlacaConductor(String driverId, LocalDate fecha) {
        return driverCloseRepository.findFirstByDriverIdAndFechaOrderByIdDesc(driverId, fecha)
            .map(DriverClose::getPlaca)
            .filter(p -> p != null && !p.isEmpty())
            .orElseGet(() -> driverCloseRepository.findFirstByDriverIdOrderByIdDesc(driverId)
                .map(DriverClose::getPlaca)
                .filter(p -> p != null && !p.isEmpty())
                .orElse(null));
    }

    private LocalDate parsearFecha(String fecha) {
        try {
            return LocalDate.parse(fecha, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private record ConductorInfo(String avatarUrl, String nombre, String telefono) {
        static final ConductorInfo EMPTY = new ConductorInfo(null, null, null);
    }

    private record EstadisticasProcesamiento(int totalProcesados, int totalOmitidos, int totalErrores, long tiempoTotal) {}

    private record CachedResponse<T>(T response, long expiresAt) {
        static <T> CachedResponse<T> of(T response) {
            return new CachedResponse<>(response, System.currentTimeMillis() + DETALLE_CACHE_TTL_MS);
        }
        static <T> CachedResponse<T> of(T response, long ttlMs) {
            return new CachedResponse<>(response, System.currentTimeMillis() + ttlMs);
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    // ─── Facturación Semanal ─────────────────────────────────────────

    private static final String[] DIAS_SEMANA = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    @Override
    public ResumenSemanalResponse obtenerResumenSemanal(String fechaInicio, String fechaFin) {
        String cacheKey = fechaInicio + "_" + fechaFin;
        CachedResponse<ResumenSemanalResponse> cached = resumenSemanalCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("[ResumenSemanal] cache hit para {}/{}", fechaInicio, fechaFin);
            return cached.response();
        }

        long t0 = System.currentTimeMillis();
        LocalDate inicio = parsearFecha(fechaInicio);
        LocalDate fin = parsearFecha(fechaFin);
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Fechas inválidas. Formato esperado: YYYY-MM-DD");
        }
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin");
        }

        long t1 = System.currentTimeMillis();
        List<CalculatedShift> turnosSemana = calculatedShiftRepository.findByFechaBetween(inicio, fin);
        long t2 = System.currentTimeMillis();
        log.info("[ResumenSemanal] findByFechaBetween shifts: {}ms, {} turnos", t2 - t1, turnosSemana.size());

        if (turnosSemana.isEmpty()) {
            ResumenSemanalResponse vacia = ResumenSemanalResponse.builder()
                .fechaInicio(inicio.toString()).fechaFin(fin.toString())
                .conductores(List.of()).build();
            resumenSemanalCache.put(cacheKey, CachedResponse.of(vacia, RESUMEN_SEMANAL_CACHE_TTL_MS));
            return vacia;
        }

        Map<String, List<CalculatedShift>> turnosPorConductor = turnosSemana.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getDriverId));

        long t3 = System.currentTimeMillis();
        Map<String, ConductorInfo> infoConductores = obtenerInfoConductores();
        long t4 = System.currentTimeMillis();
        log.info("[ResumenSemanal] obtenerInfoConductores: {}ms", t4 - t3);

        long t5 = System.currentTimeMillis();
        List<DriverClose> todosCierres = driverCloseRepository.findByFechaBetween(inicio, fin);
        long t6 = System.currentTimeMillis();
        log.info("[ResumenSemanal] findByFechaBetween cierres: {}ms, {} cierres", t6 - t5, todosCierres.size());

        Map<String, Map<LocalDate, DriverClose>> cierresPorConductor = new HashMap<>();
        for (DriverClose c : todosCierres) {
            if (c.getFecha() != null) {
                cierresPorConductor
                    .computeIfAbsent(c.getDriverId(), k -> new HashMap<>())
                    .put(c.getFecha(), c);
            }
        }

        long t7 = System.currentTimeMillis();
        Map<String, YangoIncomeSummary> incomePorConductor = fetchIncomeSummariesEnParalelo(
                turnosPorConductor.keySet(), fechaInicio, fechaFin);
        long t7b = System.currentTimeMillis();
        log.info("[ResumenSemanal] fetchIncomeSummaries: {}ms, {} conductores con datos",
                t7b - t7, incomePorConductor.size());

        List<ResumenSemanalResponse.ConductorSemanalInfo> conductoresInfo = turnosPorConductor.entrySet().stream()
            .map(e -> construirConductorSemanal(e.getKey(), e.getValue(), infoConductores,
                cierresPorConductor.getOrDefault(e.getKey(), Map.of()), inicio, fin,
                incomePorConductor.get(e.getKey())))
            .collect(Collectors.toList());
        long t8 = System.currentTimeMillis();
        log.info("[ResumenSemanal] construir conductores: {}ms, {} conductores", t8 - t7, conductoresInfo.size());

        int totalViajes = conductoresInfo.stream().mapToInt(ResumenSemanalResponse.ConductorSemanalInfo::getTotalViajes).sum();
        double totalProduccion = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getMontoTotalProducido).sum();
        double totalComision = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getComisionApp).sum();
        double totalCombustible = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getGastoCombustible).sum();
        double totalPagar = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getPagoTotal).sum();
        double totalPagado = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getTotalPagado).sum();
        double totalBonos = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getBono).sum();
        double totalUtilidad = conductoresInfo.stream().mapToDouble(ResumenSemanalResponse.ConductorSemanalInfo::getUtilidad).sum();
        int totalTurnos = conductoresInfo.stream().mapToInt(c -> c.getDatosPorDia() != null
            ? c.getDatosPorDia().stream().mapToInt(ResumenSemanalResponse.DiaSemanalInfo::getCantidadTurnos).sum() : 0).sum();

        long t9 = System.currentTimeMillis();
        log.info("[ResumenSemanal] TOTAL: {}ms", t9 - t0);

        ResumenSemanalResponse response = ResumenSemanalResponse.builder()
            .fechaInicio(inicio.toString()).fechaFin(fin.toString())
            .totalConductores(conductoresInfo.size())
            .totalViajes(totalViajes).totalProduccion(round2(totalProduccion))
            .totalComision(round2(totalComision)).totalCombustible(round2(totalCombustible))
            .totalPagar(round2(totalPagar)).totalPagado(round2(totalPagado))
            .totalPendiente(round2(Math.max(0, totalPagar - totalPagado)))
            .totalBonos(round2(totalBonos)).totalUtilidad(round2(totalUtilidad))
            .totalTurnos(totalTurnos)
            .conductores(conductoresInfo)
            .build();

        resumenSemanalCache.put(cacheKey, CachedResponse.of(response, RESUMEN_SEMANAL_CACHE_TTL_MS));
        return response;
    }

    private ResumenSemanalResponse.ConductorSemanalInfo construirConductorSemanal(
            String driverId, List<CalculatedShift> turnos, Map<String, ConductorInfo> infoConductores,
            Map<LocalDate, DriverClose> cierresPorFecha, LocalDate inicio, LocalDate fin,
            YangoIncomeSummary incomeSummary) {

        ConductorInfo info = infoConductores.getOrDefault(driverId, ConductorInfo.EMPTY);

        int totalViajes = 0;
        int viajesValidos = 0;
        double horasTrabajo = 0;
        double montoTotalProducido = 0;
        double comisionApp = 0;
        double kmRecorrido = 0;
        double gastoCombustible = 0;
        double totalPagadoAcumulado = 0;
        int diasTrabajados = 0;
        int diasLiquidados = 0;
        int diurnoCount = 0;
        int nocturnoCount = 0;

        Map<LocalDate, List<CalculatedShift>> turnosPorFecha = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getFecha));

        List<ResumenSemanalResponse.DiaSemanalInfo> datosPorDia = new ArrayList<>();

        for (LocalDate dia = inicio; !dia.isAfter(fin); dia = dia.plusDays(1)) {
            List<CalculatedShift> turnosDia = turnosPorFecha.getOrDefault(dia, List.of());
            if (turnosDia.isEmpty()) continue;

            diasTrabajados++;
            int viajesDia = turnosDia.stream().mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0).sum();
            double prodDia = sumarProduccionTotal(turnosDia);
            double comisionDia = sumarComisionesServicio(turnosDia);
            double horasDia = turnosDia.stream().mapToInt(t -> t.getDuracionMinutos() != null ? t.getDuracionMinutos() : 0).sum() / 60.0;

            totalViajes += viajesDia;
            viajesValidos += viajesDia;
            montoTotalProducido += prodDia;
            comisionApp += comisionDia;
            horasTrabajo += horasDia;

            for (CalculatedShift t : turnosDia) {
                if (t.getTipoTurno() == CalculatedShift.TipoTurno.manana) diurnoCount++;
                if (t.getTipoTurno() == CalculatedShift.TipoTurno.tarde) nocturnoCount++;
            }

            double montoPagarDia = calcularMontoTotalPagar(turnosDia);
            double montoPagadoDia = turnosDia.stream()
                .filter(t -> Boolean.TRUE.equals(t.getPagado()))
                .mapToDouble(t -> t.getMontoTotal() != null ? t.getMontoTotal() : 0.0).sum();
            totalPagadoAcumulado += montoPagadoDia;

            DriverClose cierre = cierresPorFecha.get(dia);
            double combustibleDia = 0;
            double liquidaEfectivo = 0;
            double liquidaYape = 0;
            double otrosGastos = 0;
            Integer odometroInicial = null;
            Integer odometroFinal = null;
            double kmDia = 0;
            boolean liquidado = false;

            if (cierre != null) {
                combustibleDia = toDouble(cierre.getGnvSoles()) + toDouble(cierre.getGasolinaSoles());
                liquidaEfectivo = toDouble(cierre.getLiquidaEfectivo());
                liquidaYape = toDouble(cierre.getLiquidaYape());
                otrosGastos = toDouble(cierre.getOtrosGastos());
                odometroInicial = cierre.getOdometroInicial();
                odometroFinal = cierre.getOdometroFinal();
                if (odometroInicial != null && odometroFinal != null && odometroFinal > odometroInicial) {
                    kmDia = odometroFinal - odometroInicial;
                }
                gastoCombustible += combustibleDia;
                kmRecorrido += kmDia;
                diasLiquidados++;
                liquidado = true;
            }

            String diaNombre = DIAS_SEMANA[dia.getDayOfWeek().getValue() - 1];
            StringBuilder tiposBuilder = new StringBuilder();
            for (CalculatedShift t : turnosDia) {
                if (tiposBuilder.length() > 0) tiposBuilder.append(", ");
                tiposBuilder.append(t.getTipoTurno() == CalculatedShift.TipoTurno.manana ? "D" : "N");
            }
            String turnosTipo = tiposBuilder.toString();

            datosPorDia.add(ResumenSemanalResponse.DiaSemanalInfo.builder()
                .fecha(dia.toString()).diaSemana(diaNombre)
                .cantidadViajes(viajesDia).cantidadTurnos(turnosDia.size())
                .turnosTipo(turnosTipo)
                .produccionTotal(round2(prodDia)).comisionesServicio(round2(comisionDia))
                .montoTotalPagar(round2(montoPagarDia)).montoTotalPagado(round2(montoPagadoDia))
                .gastoCombustible(round2(combustibleDia))
                .liquidaEfectivo(round2(liquidaEfectivo)).liquidaYape(round2(liquidaYape))
                .otrosGastos(round2(otrosGastos))
                .odometroInicial(odometroInicial).odometroFinal(odometroFinal)
                .kmRecorrido(round2(kmDia)).liquidado(liquidado)
                .build());
        }

        String placa = turnos.stream()
            .map(CalculatedShift::getPlaca).filter(p -> p != null && !p.isEmpty())
            .findFirst()
            .orElseGet(() -> cierresPorFecha.values().stream()
                .map(DriverClose::getPlaca).filter(p -> p != null && !p.isEmpty())
                .findFirst().orElse(null));

        double montoNeto;
        double bonoYangoAcum;
        double produccionBonificable;

        if (incomeSummary != null) {
            double prodSinBono = round2(
                    d0(incomeSummary.getCashCollected())
                    + d0(incomeSummary.getNonCashPayment())
                    + d0(incomeSummary.getCorporate())
                    + d0(incomeSummary.getPromotionCompensation())
                    + d0(incomeSummary.getTips()));
            bonoYangoAcum = round2(d0(incomeSummary.getBonificacion()));
            double comisionIncome = round2(Math.abs(d0(incomeSummary.getPlatformFees())));
            double nuevoTotalProducido = prodSinBono;

            if (Math.abs(nuevoTotalProducido - montoTotalProducido) > 0.01
                    || Math.abs(comisionIncome - comisionApp) > 0.01) {
                log.info("[ResumenSemanal] driver={} usando income card: prod={}→{} comision={}→{} bonoYango={}",
                        driverId, montoTotalProducido, nuevoTotalProducido,
                        comisionApp, comisionIncome, bonoYangoAcum);
                montoTotalProducido = nuevoTotalProducido;
                comisionApp = comisionIncome;
            }
            montoNeto = round2(montoTotalProducido - comisionApp);
        } else {
            bonoYangoAcum = 0;
            montoNeto = round2(montoTotalProducido - comisionApp);
        }

        double tph = horasTrabajo > 0 ? round2(totalViajes / horasTrabajo) : 0;
        double gastoMantenimiento = round2(kmRecorrido * 0.15);
        produccionBonificable = round2(montoNeto + bonoYangoAcum - (gastoCombustible + gastoMantenimiento));
        int bonoAdicViajes = calcularBonoAdic(totalViajes, inicio);
        double bono = round2(produccionBonificable - bonoAdicViajes);
        double porcentajePago = calcularPorcentaje(viajesValidos, inicio);
        double pago = round2(bono * porcentajePago);
        double pagoTotal = round2(pago + bonoAdicViajes);
        double utilidad = round2(pagoTotal - bono);
        double utilidadPorViaje = totalViajes > 0 ? round2(utilidad / totalViajes) : 0;
        double pagoPorViaje = totalViajes > 0 ? round2(pagoTotal / totalViajes) : 0;

        String turno;
        if (diurnoCount > 0 && nocturnoCount == 0) turno = "diurno";
        else if (nocturnoCount > 0 && diurnoCount == 0) turno = "nocturno";
        else if (diurnoCount > 0 && nocturnoCount > 0) turno = "D, N";
        else turno = "diurno";

        boolean completamenteLiquidado = montoTotalProducido > 0 && diasLiquidados >= diasTrabajados;

        return ResumenSemanalResponse.ConductorSemanalInfo.builder()
            .driverId(driverId).avatarUrl(info.avatarUrl()).nombre(info.nombre())
            .telefono(info.telefono()).placa(placa).turno(turno)
            .diasTrabajados(diasTrabajados).diasLiquidados(diasLiquidados)
            .totalViajes(totalViajes).viajesValidos(viajesValidos)
            .horasTrabajo(round2(horasTrabajo)).tph(tph)
            .montoTotalProducido(round2(montoTotalProducido)).comisionApp(round2(comisionApp))
            .montoNeto(montoNeto).kmRecorrido(round2(kmRecorrido))
            .gastoCombustible(round2(gastoCombustible))
            .bonoYango(bonoYangoAcum)
            .gastoMantenimiento(gastoMantenimiento)
            .produccionBonificable(produccionBonificable)
            .bonoAdicViajes(bonoAdicViajes).bono(bono)
            .porcentajePago(porcentajePago).pago(pago)
            .pagoTotal(pagoTotal)
            .totalPagado(round2(totalPagadoAcumulado))
            .utilidad(utilidad).utilidadPorViaje(utilidadPorViaje).pagoPorViaje(pagoPorViaje)
            .completamenteLiquidado(completamenteLiquidado)
            .datosPorDia(datosPorDia)
            .build();
    }

    private int calcularBonoAdic(int totalViajes, LocalDate fechaReferencia) {
        List<BonusThreshold> thresholds = bonusThresholdRepository.findApplicableForDate(fechaReferencia);
        for (BonusThreshold t : thresholds) {
            if (totalViajes >= t.getMinTrips()) return t.getBonusAmount().intValue();
        }
        return 0;
    }

    /**
     * Busca en paralelo el resumen de ingresos Yango corregido para cada conductor de la semana.
     * Usa {@link YangoWeeklyService#fetchCorrectedWeeklyIncomeSummary}.
     * Si falla para un conductor, se omite sin interrumpir al resto.
     */
    private Map<String, YangoIncomeSummary> fetchIncomeSummariesEnParalelo(
            java.util.Set<String> driverIds, String fechaInicio, String fechaFin) {
        Map<String, YangoIncomeSummary> resultado = new java.util.concurrent.ConcurrentHashMap<>();
        if (driverIds == null || driverIds.isEmpty()) return resultado;

        String dateFrom = fechaInicio + "T00:00:00-05:00";
        String dateTo = fechaFin + "T23:59:59-05:00";
        String pid = this.parkId;

        java.util.List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        for (String driverId : driverIds) {
            if (driverId == null || driverId.isBlank()) continue;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    yangoWeeklyService.fetchCorrectedWeeklyIncomeSummary(driverId, pid, dateFrom, dateTo)
                            .ifPresent(summary -> resultado.put(driverId, summary));
                } catch (Exception e) {
                    log.debug("[ResumenSemanal] incomeSummary falló para driver={}: {}",
                            driverId, e.getMessage());
                }
            }, shiftExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("[ResumenSemanal] error en batch fetchIncomeSummaries: {}", e.getMessage());
        }

        return resultado;
    }

    private double calcularPorcentaje(int viajesValidos, LocalDate fechaReferencia) {
        List<PaymentPercentage> percentages = paymentPercentageRepository.findApplicableForDate(fechaReferencia);
        for (PaymentPercentage p : percentages) {
            if (viajesValidos >= p.getMinValidatedTrips()) return p.getPercentage();
        }
        return 0.2;
    }

    private static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private static double d0(Double v) {
        return v == null ? 0.0 : v;
    }

    private static BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(round2(value));
    }

    private static BigDecimal toBigDecimal(int value) {
        return BigDecimal.valueOf(value);
    }

    @Override
    public FacturacionSemanal registrarFacturacionSemanal(FacturacionSemanal facturacion) {
        Optional<FacturacionSemanal> existente = facturacionSemanalRepository
            .findByDriverIdAndFechaInicioAndFechaFin(
                facturacion.getDriverId(), facturacion.getFechaInicio(), facturacion.getFechaFin());

        if (existente.isPresent()) {
            FacturacionSemanal existing = existente.get();
            facturacion.setId(existing.getId());
            if (facturacion.getUserId() != null) {
                existing.setUserId(facturacion.getUserId());
            }
            if (!"pagado".equals(existing.getEstado())) {
                existing.setEstado("pagado");
            }
            existing.setPagoTotal(facturacion.getPagoTotal());
            existing.setUtilidad(facturacion.getUtilidad());
            existing.setBono(facturacion.getBono());
            existing.setPago(facturacion.getPago());
            existing.setDiasLiquidados(facturacion.getDiasLiquidados());
            existing.setDiasTrabajados(facturacion.getDiasTrabajados());
            existing.setTotalViajes(facturacion.getTotalViajes());
            existing.setMontoTotalProducido(facturacion.getMontoTotalProducido());
            existing.setComisionApp(facturacion.getComisionApp());
            existing.setMontoNeto(facturacion.getMontoNeto());
            existing.setGastoCombustible(facturacion.getGastoCombustible());
            existing.setBonoYango(facturacion.getBonoYango());
            existing.setGastoMantenimiento(facturacion.getGastoMantenimiento());
            existing.setProduccionBonificable(facturacion.getProduccionBonificable());
            existing.setBonoAdicViajes(facturacion.getBonoAdicViajes());
            existing.setPorcentajePago(facturacion.getPorcentajePago());
            existing.setHorasTrabajo(facturacion.getHorasTrabajo());
            existing.setKmRecorrido(facturacion.getKmRecorrido());
            existing.setViajesValidos(facturacion.getViajesValidos());
            existing.setUtilidadPorViaje(facturacion.getUtilidadPorViaje());
            existing.setPagoPorViaje(facturacion.getPagoPorViaje());
            existing.setTurno(facturacion.getTurno());
            return facturacionSemanalRepository.save(existing);
        }

        return facturacionSemanalRepository.save(facturacion);
    }

    @Override
    public List<FacturacionSemanal> obtenerHistorialFacturacion(String fechaInicio, String fechaFin) {
        LocalDate inicio = parsearFecha(fechaInicio);
        LocalDate fin = parsearFecha(fechaFin);
        if (inicio != null && fin != null && !inicio.isAfter(fin)) {
            return facturacionSemanalRepository.findByRangoFechas(inicio, fin);
        }
        if (inicio != null && fin != null) {
            return facturacionSemanalRepository.findByRangoFechas(fin, inicio);
        }
        return facturacionSemanalRepository.findAllByOrderByFechaInicioDesc();
    }

    @Override
    public BillingConfigResponse obtenerConfiguracionBilling() {
        return BillingConfigResponse.builder()
            .bonusThresholds(bonusThresholdRepository.findAll())
            .paymentPercentages(paymentPercentageRepository.findAll())
            .build();
    }

    @Override
    public BillingConfigResponse guardarConfiguracionBilling(BillingConfigResponse config, Long userId) {
        if (config.getBonusThresholds() != null) {
            config.getBonusThresholds().forEach(t -> {
                if (t.getId() == null) t.setUpdatedBy(userId);
                else t.setUpdatedBy(userId);
            });
            bonusThresholdRepository.saveAll(config.getBonusThresholds());
        }
        if (config.getPaymentPercentages() != null) {
            config.getPaymentPercentages().forEach(p -> p.setUpdatedBy(userId));
            paymentPercentageRepository.saveAll(config.getPaymentPercentages());
        }
        return obtenerConfiguracionBilling();
    }
}
