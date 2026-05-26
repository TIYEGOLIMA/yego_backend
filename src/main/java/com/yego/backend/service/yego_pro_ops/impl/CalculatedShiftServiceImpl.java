package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_api_externo.api.response.YangoIncomeSummary;
import com.yego.backend.entity.yego_pro_ops.api.response.BillingConfigResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ResumenSemanalResponse;
import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import com.yego.backend.entity.yego_pro_ops.entities.WeeklyIncome;
import com.yego.backend.repository.yego_pro_ops.BonusThresholdRepository;
import com.yego.backend.repository.yego_pro_ops.CalculatedShiftRepository;
import com.yego.backend.repository.yego_pro_ops.DriverCloseRepository;
import com.yego.backend.repository.yego_pro_ops.FacturacionSemanalRepository;
import com.yego.backend.repository.yego_pro_ops.PaymentPercentageRepository;
import com.yego.backend.repository.yego_pro_ops.WeeklyIncomeRepository;
import com.yego.backend.service.yego_api_externo.YangoWeeklyService;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CalculatedShiftServiceImpl implements CalculatedShiftService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final int EXECUTOR_THREADS = 10;
    private static final long INCOME_SUMMARY_TIMEOUT_SECONDS = 30;

    private final CalculatedShiftRepository calculatedShiftRepository;
    private final FacturacionSemanalRepository facturacionSemanalRepository;
    private final PaymentPercentageRepository paymentPercentageRepository;
    private final BonusThresholdRepository bonusThresholdRepository;
    private final WeeklyIncomeRepository weeklyIncomeRepository;
    private final DriverCloseRepository driverCloseRepository;
    private final FleetDriverService fleetDriverService;
    private final DriverOrdersService driverOrdersService;
    private final YangoWeeklyService yangoWeeklyService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(EXECUTOR_THREADS);

    private final ConcurrentHashMap<String, Cached<List<CalculatedShift>>> turnosPorFechaCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Cached<DriverPaymentSummaryResponse>> resumenPagosCache = new ConcurrentHashMap<>();

    public CalculatedShiftServiceImpl(
            CalculatedShiftRepository calculatedShiftRepository,
            FacturacionSemanalRepository facturacionSemanalRepository,
            PaymentPercentageRepository paymentPercentageRepository,
            BonusThresholdRepository bonusThresholdRepository,
            WeeklyIncomeRepository weeklyIncomeRepository,
            DriverCloseRepository driverCloseRepository,
            FleetDriverService fleetDriverService,
            DriverOrdersService driverOrdersService,
            YangoWeeklyService yangoWeeklyService) {
        this.calculatedShiftRepository = calculatedShiftRepository;
        this.facturacionSemanalRepository = facturacionSemanalRepository;
        this.paymentPercentageRepository = paymentPercentageRepository;
        this.bonusThresholdRepository = bonusThresholdRepository;
        this.weeklyIncomeRepository = weeklyIncomeRepository;
        this.driverCloseRepository = driverCloseRepository;
        this.fleetDriverService = fleetDriverService;
        this.driverOrdersService = driverOrdersService;
        this.yangoWeeklyService = yangoWeeklyService;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void procesarHorasTurnoDiaAnterior() {
        LocalDate fechaAnterior = LocalDate.now(LIMA_ZONE).minusDays(1);
        log.info("[CalculatedShiftService] procesando turnos día anterior fecha={}", fechaAnterior);

        try {
            DriverSimpleResponse driverList = fleetDriverService.obtenerListaConductoresSimplificada();
            if (driverList == null || driverList.getConductores() == null || driverList.getConductores().isEmpty()) {
                log.warn("[CalculatedShiftService] sin conductores para procesar fecha={}", fechaAnterior);
                return;
            }

            List<CompletableFuture<Void>> futures = driverList.getConductores().stream()
                .map(driver -> CompletableFuture.runAsync(() -> {
                    try {
                        obtenerOCalcularTurnos(driver.getDriverId(), fechaAnterior.format(DATE_FORMATTER));
                    } catch (Exception e) {
                        log.warn("[CalculatedShiftService] error procesando driverId={} fecha={}: {}",
                            driver.getDriverId(), fechaAnterior, e.getMessage());
                    }
                }, executorService))
                .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("[CalculatedShiftService] batch completado fecha={} conductores={}",
                fechaAnterior, driverList.getConductores().size());
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error batch fecha={}: {}", fechaAnterior, e.getMessage(), e);
        }
    }

    @Override
    public FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId) {
        List<CalculatedShift> turnos = calculatedShiftRepository.findByDriverIdOrderByFecha(driverId);
        if (turnos.isEmpty()) {
            return FechasConTiposTurnoResponse.builder()
                .driverId(driverId)
                .fechas(new ArrayList<>())
                .build();
        }

        Map<LocalDate, List<FechasConTiposTurnoResponse.TipoTurnoInfo>> fechaMap = new HashMap<>();
        for (CalculatedShift t : turnos) {
            fechaMap.computeIfAbsent(t.getFecha(), k -> new ArrayList<>())
                .add(FechasConTiposTurnoResponse.TipoTurnoInfo.builder()
                    .id(t.getId())
                    .tipoTurno(t.getTipoTurno().name())
                    .build());
        }

        List<FechasConTiposTurnoResponse.FechaConTiposTurno> fechas = fechaMap.entrySet().stream()
            .sorted(Map.Entry.<LocalDate, List<FechasConTiposTurnoResponse.TipoTurnoInfo>>comparingByKey().reversed())
            .map(e -> FechasConTiposTurnoResponse.FechaConTiposTurno.builder()
                .fecha(e.getKey())
                .tiposTurno(e.getValue())
                .build())
            .collect(Collectors.toList());

        return FechasConTiposTurnoResponse.builder()
            .driverId(driverId)
            .fechas(fechas)
            .build();
    }

    @Override
    public DriverPaymentSummaryResponse obtenerResumenPagos(String fecha) {
        Cached<DriverPaymentSummaryResponse> cached = resumenPagosCache.get(fecha);
        if (cached != null && !cached.expired()) {
            return cached.value();
        }

        LocalDate fechaLocal = LocalDate.parse(fecha, DATE_FORMATTER);
        List<CalculatedShift> turnos = calculatedShiftRepository.findByFechaOrderByDriverId(fechaLocal);

        Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap = obtenerMapaConductores();

        Map<String, List<CalculatedShift>> turnosPorConductor = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getDriverId));

        List<DriverPaymentSummaryResponse.ConductorPaymentInfo> conductores = new ArrayList<>();
        for (Map.Entry<String, List<CalculatedShift>> entry : turnosPorConductor.entrySet()) {
            String driverId = entry.getKey();
            List<CalculatedShift> turnosConductor = entry.getValue();
            DriverSimpleResponse.DriverInfo info = driverInfoMap.get(driverId);

            double produccionTotal = turnosConductor.stream()
                .mapToDouble(t -> t.getProduccionTotal() != null ? t.getProduccionTotal() : 0).sum();
            double comisionesServicio = turnosConductor.stream()
                .mapToDouble(t -> t.getComisionesServicio() != null ? t.getComisionesServicio() : 0).sum();
            int cantidadViajes = turnosConductor.stream()
                .mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0).sum();
            int duracionTotalMin = turnosConductor.stream()
                .mapToInt(t -> t.getDuracionMinutos() != null ? t.getDuracionMinutos() : 0).sum();
            double viajesPorHora = duracionTotalMin > 0
                ? round2((double) cantidadViajes / (duracionTotalMin / 60.0))
                : 0;

            List<DriverPaymentSummaryResponse.TurnoInfo> turnosInfo = turnosConductor.stream()
                .map(t -> DriverPaymentSummaryResponse.TurnoInfo.builder()
                    .id(t.getId())
                    .fecha(t.getFecha().format(DATE_FORMATTER))
                    .horaInicio(t.getHoraInicio() != null ? t.getHoraInicio().format(DATETIME_FORMATTER) : null)
                    .horaFin(t.getHoraFin() != null ? t.getHoraFin().format(DATETIME_FORMATTER) : null)
                    .tipoTurno(t.getTipoTurno().name())
                    .duracionMinutos(t.getDuracionMinutos())
                    .montoTotal(t.getMontoTotal())
                    .pagado(t.getPagado())
                    .build())
                .collect(Collectors.toList());

            conductores.add(DriverPaymentSummaryResponse.ConductorPaymentInfo.builder()
                .driverId(driverId)
                .avatarUrl(info != null ? info.getAvatarUrl() : null)
                .nombre(info != null ? info.getNombre() : null)
                .telefono(info != null ? info.getTelefono() : null)
                .placa(turnosConductor.get(0).getPlaca())
                .montoTotalPagar(round2(produccionTotal))
                .produccionTotal(round2(produccionTotal))
                .comisionesServicio(round2(comisionesServicio))
                .cantidadTurnos(turnosConductor.size())
                .cantidadViajes(cantidadViajes)
                .viajesPorHora(viajesPorHora)
                .turnos(turnosInfo)
                .build());
        }

        DriverPaymentSummaryResponse result = DriverPaymentSummaryResponse.builder()
            .conductores(conductores)
            .build();

        resumenPagosCache.put(fecha, Cached.of(result, 60000L));
        return result;
    }

    @Override
    public PaidShiftsResponse obtenerTurnosPagados(String fecha) {
        List<CalculatedShift> turnos;
        if (fecha != null && !fecha.isEmpty()) {
            LocalDate fechaLocal = LocalDate.parse(fecha, DATE_FORMATTER);
            turnos = calculatedShiftRepository.findByPagadoTrueAndFecha(fechaLocal);
        } else {
            turnos = calculatedShiftRepository.findByPagadoTrue();
        }

        return construirPaidShiftsResponse(turnos);
    }

    @Override
    public List<CalculatedShift> obtenerOCalcularTurnos(String driverId, String fecha) {
        LocalDate fechaLocal = LocalDate.parse(fecha, DATE_FORMATTER);
        List<CalculatedShift> existentes = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fechaLocal);
        if (!existentes.isEmpty()) {
            return existentes;
        }

        try {
            List<CalculatedShift> calculados = calcularTurnosAsync(driverId, fecha).get(30, TimeUnit.SECONDS);
            return calculados != null ? calculados : new ArrayList<>();
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error calcular turnos driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public CompletableFuture<List<CalculatedShift>> calcularTurnosAsync(String driverId, String fecha) {
        return CompletableFuture.supplyAsync(() -> calcularTurnosSync(driverId, fecha), executorService);
    }

    @Override
    public CompletableFuture<List<CalculatedShift>> recalcularTurnos(String driverId, String fecha) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate fechaLocal = LocalDate.parse(fecha, DATE_FORMATTER);
            calculatedShiftRepository.deleteByDriverIdAndFecha(driverId, fechaLocal);
            turnosPorFechaCache.remove(fecha);
            resumenPagosCache.remove(fecha);
            return calcularTurnosSync(driverId, fecha);
        }, executorService);
    }

    private List<CalculatedShift> calcularTurnosSync(String driverId, String fecha) {
        LocalDate fechaLocal = LocalDate.parse(fecha, DATE_FORMATTER);

        List<CalculatedShift> existentes = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fechaLocal);
        if (!existentes.isEmpty()) {
            return existentes;
        }

        List<CalculatedShift> turnos = new ArrayList<>();

        try {
            String dateFrom = fechaLocal.atStartOfDay(LIMA_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            String dateTo = fechaLocal.atTime(23, 59, 59).atZone(LIMA_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            DriverOrdersResponse viajesResponse = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo);
            if (viajesResponse == null || viajesResponse.getOrders() == null || viajesResponse.getOrders().isEmpty()) {
                log.info("[CalculatedShiftService] sin viajes para driverId={} fecha={}", driverId, fecha);
                return turnos;
            }

            List<ViajeTemporal> viajes = viajesResponse.getOrders().stream()
                .filter(t -> t.getEndedAt() != null)
                .map(t -> {
                    LocalDateTime endedAt = null;
                    try {
                        String endedStr = t.getEndedAt();
                        if (endedStr.contains("T")) {
                            endedAt = LocalDateTime.parse(endedStr.substring(0, 19).replace("T", " "), DATETIME_FORMATTER);
                        } else {
                            endedAt = LocalDateTime.parse(endedStr, DATETIME_FORMATTER);
                        }
                    } catch (Exception ignored) {}
                    return new ViajeTemporal(t, endedAt);
                })
                .filter(v -> v.endedAt != null)
                .sorted(Comparator.comparing(v -> v.endedAt))
                .collect(Collectors.toList());

            if (viajes.isEmpty()) {
                return turnos;
            }

            int horaCorte = 14;
            CalculatedShift.TipoTurno tipoActual = null;
            LocalDateTime horaInicio = null;
            LocalDateTime horaFin = null;
            int viajesTurno = 0;
            double produccionTurno = 0;
            double comisionesTurno = 0;
            double montoTotalTurno = 0;

            for (ViajeTemporal v : viajes) {
                CalculatedShift.TipoTurno tipoViaje = v.endedAt.getHour() < horaCorte
                    ? CalculatedShift.TipoTurno.manana : CalculatedShift.TipoTurno.tarde;

                if (tipoActual == null) {
                    tipoActual = tipoViaje;
                    horaInicio = v.endedAt;
                } else if (tipoActual != tipoViaje) {
                    CalculatedShift turno = crearTurnoCalculado(driverId, fechaLocal, tipoActual,
                        horaInicio, horaFin, viajesTurno, produccionTurno, comisionesTurno, montoTotalTurno);
                    turnos.add(calculatedShiftRepository.save(turno));

                    tipoActual = tipoViaje;
                    horaInicio = v.endedAt;
                    viajesTurno = 0;
                    produccionTurno = 0;
                    comisionesTurno = 0;
                    montoTotalTurno = 0;
                }

                viajesTurno++;
                horaFin = v.endedAt;
                produccionTurno += v.order.getPrice() != null ? v.order.getPrice() : 0;
                comisionesTurno += v.order.getPriceCommissionService() != null ? v.order.getPriceCommissionService() : 0;
            }

            if (tipoActual != null) {
                CalculatedShift turno = crearTurnoCalculado(driverId, fechaLocal, tipoActual,
                    horaInicio, horaFin, viajesTurno, produccionTurno, comisionesTurno, montoTotalTurno);
                turnos.add(calculatedShiftRepository.save(turno));
            }
        } catch (Exception e) {
            log.error("[CalculatedShiftService] error calculando turnos driverId={} fecha={}: {}",
                driverId, fecha, e.getMessage(), e);
        }

        turnosPorFechaCache.remove(fecha);
        resumenPagosCache.remove(fecha);
        return turnos;
    }

    private CalculatedShift crearTurnoCalculado(String driverId, LocalDate fecha, CalculatedShift.TipoTurno tipoTurno,
            LocalDateTime horaInicio, LocalDateTime horaFin, int cantidadViajes,
            double produccionTotal, double comisionesServicio, double montoTotal) {
        int duracionMinutos = horaInicio != null && horaFin != null
            ? (int) java.time.Duration.between(horaInicio, horaFin).toMinutes()
            : 0;

        return CalculatedShift.builder()
            .driverId(driverId)
            .fecha(fecha)
            .horaInicio(horaInicio)
            .horaFin(horaFin)
            .tipoTurno(tipoTurno)
            .estado(CalculatedShift.EstadoTurno.activo)
            .duracionMinutos(duracionMinutos)
            .cantidadViajes(cantidadViajes)
            .montoTotal(round2(montoTotal))
            .produccionTotal(round2(produccionTotal))
            .comisionesServicio(round2(comisionesServicio))
            .pagado(false)
            .esManual(false)
            .build();
    }

    @Override
    public void invalidarCacheDetalle(String fecha) {
        turnosPorFechaCache.remove(fecha);
        resumenPagosCache.remove(fecha);
    }

    @Override
    public ResumenSemanalResponse obtenerResumenSemanal(String fechaInicio, String fechaFin) {
        LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
        LocalDate fin = LocalDate.parse(fechaFin, DATE_FORMATTER);

        List<FacturacionSemanal> snapshot = facturacionSemanalRepository.findByFechaInicioAndFechaFin(inicio, fin);
        if (snapshot != null && !snapshot.isEmpty()) {
            List<CalculatedShift> turnos = calculatedShiftRepository.findByFechaBetween(inicio, fin);
            Map<String, DriverClose> cierresPorDriver = obtenerCierresEnRango(inicio, fin);
            log.info("[ResumenSemanal] usando snapshot BD para {}/{} ({} conductores, 0 llamadas Yango)",
                fechaInicio, fechaFin, snapshot.size());
            return construirResumenDesdeBD(snapshot, turnos, cierresPorDriver);
        }

        List<CalculatedShift> turnos = calculatedShiftRepository.findByFechaBetween(inicio, fin);
        if (turnos.isEmpty()) {
            return ResumenSemanalResponse.builder()
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .totalConductores(0)
                .totalViajes(0)
                .totalProduccion(0)
                .totalComision(0)
                .totalCombustible(0)
                .totalPagar(0)
                .totalPagado(0)
                .totalPendiente(0)
                .totalBonos(0)
                .totalUtilidad(0)
                .totalTurnos(0)
                .conductores(new ArrayList<>())
                .build();
        }

        Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap = obtenerMapaConductores();
        Map<String, List<CalculatedShift>> turnosPorConductor = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getDriverId));

        String dateFromIso = inicio.atStartOfDay(LIMA_ZONE).format(ISO_OFFSET_FORMATTER);
        String dateToIso = fin.atTime(23, 59, 59).atZone(LIMA_ZONE).format(ISO_OFFSET_FORMATTER);
        Map<String, Optional<YangoIncomeSummary>> incomeSummaries = fetchIncomeSummariesEnParalelo(
            turnosPorConductor, dateFromIso, dateToIso);

        String fechaLunes = inicio.format(DATE_FORMATTER);
        String fechaLunesSig = inicio.plusDays(7).format(DATE_FORMATTER);
        String fechaDomingo = fin.format(DATE_FORMATTER);
        Map<String, Optional<YangoIncomeSummary>> incomeExacto = fetchIncomeSummariesExacto(
            turnosPorConductor, fechaLunes, fechaDomingo);

        Map<String, Double> txLunesMap = new HashMap<>();
        Map<String, Double> txLunesSigMap = new HashMap<>();
        for (String driverId : turnosPorConductor.keySet()) {
            yangoWeeklyService.fetchFirstBonusTransactionAmount(driverId, null, fechaLunes)
                .ifPresent(v -> txLunesMap.put(driverId, v));
            yangoWeeklyService.fetchFirstBonusTransactionAmount(driverId, null, fechaLunesSig)
                .ifPresent(v -> txLunesSigMap.put(driverId, v));
        }

        Map<String, DriverClose> cierresPorDriver = obtenerCierresEnRango(inicio, fin);
        List<PaymentPercentage> porcentajes = paymentPercentageRepository.findApplicableForDate(inicio);
        List<BonusThreshold> bonos = bonusThresholdRepository.findApplicableForDate(inicio);

        List<ResumenSemanalResponse.ConductorSemanalInfo> conductores = new ArrayList<>();

        for (Map.Entry<String, List<CalculatedShift>> entry : turnosPorConductor.entrySet()) {
            String driverId = entry.getKey();
            List<CalculatedShift> turnosConductor = entry.getValue();
            DriverSimpleResponse.DriverInfo info = driverInfoMap.get(driverId);

            Optional<YangoIncomeSummary> incomeOpt = incomeSummaries.getOrDefault(driverId, Optional.empty());
            YangoIncomeSummary incomeSummary = incomeOpt.orElse(null);
            Optional<YangoIncomeSummary> incomeExactoOpt = incomeExacto.getOrDefault(driverId, Optional.empty());
            double bonoSemana = incomeExactoOpt.map(i -> i.getBonificacion() != null ? i.getBonificacion() : 0).orElse(0.0);
            double bonoLunes = txLunesMap.getOrDefault(driverId, 0.0);
            double bonoLunesSig = txLunesSigMap.getOrDefault(driverId, 0.0);
            double bonoYangoAjustado = round2(bonoSemana - bonoLunes + bonoLunesSig);
            log.info("[ResumenSemanal] driver={} bono: incomeCard(lu-do)={} - txLunes={} + txLunesSig={} = {}",
                    driverId, bonoSemana, bonoLunes, bonoLunesSig, bonoYangoAjustado);

            List<ResumenSemanalResponse.DiaSemanalInfo> datosPorDia = construirDatosPorDia(turnosConductor, cierresPorDriver);
            int diasTrabajados = datosPorDia.size();
            int diasLiquidados = (int) datosPorDia.stream().filter(ResumenSemanalResponse.DiaSemanalInfo::isLiquidado).count();
            boolean completamenteLiquidado = diasLiquidados == diasTrabajados && diasTrabajados > 0;

            int totalViajes = turnosConductor.stream().mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0).sum();
            double horasTrabajo = turnosConductor.stream().mapToInt(t -> t.getDuracionMinutos() != null ? t.getDuracionMinutos() : 0).sum() / 60.0;
            double tph = horasTrabajo > 0 ? round2((double) totalViajes / horasTrabajo) : 0;
            double produccionTotal = turnosConductor.stream().mapToDouble(t -> t.getProduccionTotal() != null ? t.getProduccionTotal() : 0).sum();
            double comisionesServicio = turnosConductor.stream().mapToDouble(t -> t.getComisionesServicio() != null ? t.getComisionesServicio() : 0).sum();
            double montoNeto = produccionTotal + comisionesServicio;

            double kmRecorrido = datosPorDia.stream().mapToDouble(ResumenSemanalResponse.DiaSemanalInfo::getKmRecorrido).sum();
            double gastoCombustible = datosPorDia.stream().mapToDouble(ResumenSemanalResponse.DiaSemanalInfo::getGastoCombustible).sum();
            double gastoMantenimiento = round2(kmRecorrido * 0.15);
            boolean usaFleetTotal = incomeSummary != null && incomeSummary.getTotal() != null;
            double produccionBonificable = usaFleetTotal
                ? round2(montoNeto - (gastoCombustible + gastoMantenimiento))
                : round2(montoNeto + bonoYangoAjustado - (gastoCombustible + gastoMantenimiento));

            int bonoAdicViajes = 0;
            for (BonusThreshold b : bonos) {
                if (totalViajes >= b.getMinTrips()) {
                    bonoAdicViajes = b.getBonusAmount() != null ? b.getBonusAmount().intValue() : 0;
                    break;
                }
            }
            double bono = round2(produccionBonificable - bonoAdicViajes);

            double pct = porcentajes.stream()
                .filter(p -> totalViajes >= p.getMinValidatedTrips())
                .map(PaymentPercentage::getPercentage)
                .findFirst().orElse(0.2);
            double pago = round2(bono * pct);
            double pagoTotal = round2(pago + bonoAdicViajes);
            double totalPagado = pagoTotal;
            double utilidad = round2(pagoTotal - bono);
            double utilidadPorViaje = totalViajes > 0 ? round2(utilidad / totalViajes) : 0;
            double pagoPorViaje = totalViajes > 0 ? round2(pagoTotal / totalViajes) : 0;

            String turnoStr = turnosConductor.stream()
                .map(t -> t.getTipoTurno().name().equals("manana") ? "D" : "N")
                .distinct()
                .collect(Collectors.joining(", "));

            conductores.add(construirConductorSemanal(
                driverId, info, turnosConductor, turnoStr,
                diasTrabajados, diasLiquidados, completamenteLiquidado,
                totalViajes, horasTrabajo, tph,
                produccionTotal, comisionesServicio, montoNeto,
                kmRecorrido, gastoCombustible,
                bonoYangoAjustado, gastoMantenimiento, produccionBonificable,
                bonoAdicViajes, bono, pct, pago, pagoTotal, totalPagado,
                utilidad, utilidadPorViaje, pagoPorViaje,
                datosPorDia, incomeSummary));
        }

        double totalProduccion = conductores.stream().mapToDouble(c -> c.getMontoTotalProducido()).sum();
        double totalComision = conductores.stream().mapToDouble(c -> c.getComisionApp()).sum();
        double totalCombustible = conductores.stream().mapToDouble(c -> c.getGastoCombustible()).sum();
        double totalPagar = conductores.stream().mapToDouble(c -> c.getPagoTotal()).sum();
        double totalPagado = conductores.stream().mapToDouble(c -> c.getTotalPagado()).sum();
        double totalBonos = conductores.stream().mapToDouble(c -> c.getBono()).sum();
        double totalUtilidad = conductores.stream().mapToDouble(c -> c.getUtilidad()).sum();
        int totalTurnos = conductores.stream().mapToInt(c -> c.getDatosPorDia() != null
            ? c.getDatosPorDia().stream().mapToInt(d -> d.getCantidadTurnos()).sum() : 0).sum();
        int totalViajes = conductores.stream().mapToInt(c -> c.getTotalViajes()).sum();

        return ResumenSemanalResponse.builder()
            .fechaInicio(fechaInicio)
            .fechaFin(fechaFin)
            .totalConductores(conductores.size())
            .totalViajes(totalViajes)
            .totalProduccion(round2(totalProduccion))
            .totalComision(round2(totalComision))
            .totalCombustible(round2(totalCombustible))
            .totalPagar(round2(totalPagar))
            .totalPagado(round2(totalPagado))
            .totalPendiente(round2(Math.max(0, totalPagar - totalPagado)))
            .totalBonos(round2(totalBonos))
            .totalUtilidad(round2(totalUtilidad))
            .totalTurnos(totalTurnos)
            .conductores(conductores)
            .build();
    }

    private ResumenSemanalResponse.ConductorSemanalInfo construirConductorSemanal(
            String driverId, DriverSimpleResponse.DriverInfo info,
            List<CalculatedShift> turnosConductor, String turnoStr,
            int diasTrabajados, int diasLiquidados, boolean completamenteLiquidado,
            int totalViajes, double horasTrabajo, double tph,
            double produccionTotal, double comisionesServicio, double montoNeto,
            double kmRecorrido, double gastoCombustible,
            double bonoYango, double gastoMantenimiento, double produccionBonificable,
            double bonoAdicViajes, double bono, double pct, double pago,
            double pagoTotal, double totalPagado,
            double utilidad, double utilidadPorViaje, double pagoPorViaje,
            List<ResumenSemanalResponse.DiaSemanalInfo> datosPorDia,
            YangoIncomeSummary incomeSummary) {

        double finalProduccion = produccionTotal;
        double finalComision = comisionesServicio;
        double finalMontoNeto = montoNeto;

        if (incomeSummary != null && incomeSummary.getTotal() != null) {
            double prodFleet = round2(
                (incomeSummary.getCashCollected() != null ? incomeSummary.getCashCollected() : 0)
                + (incomeSummary.getNonCashPayment() != null ? incomeSummary.getNonCashPayment() : 0)
                + (incomeSummary.getCorporate() != null ? incomeSummary.getCorporate() : 0)
                + (incomeSummary.getPromotionCompensation() != null ? incomeSummary.getPromotionCompensation() : 0)
                + (incomeSummary.getTips() != null ? incomeSummary.getTips() : 0));
            double bonoRaw = incomeSummary.getBonificacion() != null ? incomeSummary.getBonificacion() : 0;
            finalProduccion = round2(prodFleet + bonoRaw);
            finalComision = round2(incomeSummary.getPlatformFees() != null ? incomeSummary.getPlatformFees() : 0);
            finalMontoNeto = round2(finalProduccion + finalComision);
        }

        ResumenSemanalResponse.ConductorSemanalInfo.ConductorSemanalInfoBuilder builder =
            ResumenSemanalResponse.ConductorSemanalInfo.builder()
                .driverId(driverId)
                .avatarUrl(info != null ? info.getAvatarUrl() : null)
                .nombre(info != null ? info.getNombre() : null)
                .telefono(info != null ? info.getTelefono() : null)
                .placa(turnosConductor.get(0).getPlaca())
                .turno(turnoStr)
                .diasTrabajados(diasTrabajados)
                .diasLiquidados(diasLiquidados)
                .totalViajes(totalViajes)
                .viajesValidos(totalViajes)
                .horasTrabajo(round2(horasTrabajo))
                .tph(tph)
                .montoTotalProducido(round2(finalProduccion))
                .comisionApp(round2(finalComision))
                .montoNeto(round2(finalMontoNeto))
                .kmRecorrido(round2(kmRecorrido))
                .gastoCombustible(round2(gastoCombustible))
                .bonoYango(round2(bonoYango))
                .gastoMantenimiento(round2(gastoMantenimiento))
                .produccionBonificable(round2(produccionBonificable))
                .bonoAdicViajes(round2(bonoAdicViajes))
                .bono(round2(bono))
                .porcentajePago(pct)
                .pago(round2(pago))
                .pagoTotal(round2(pagoTotal))
                .totalPagado(round2(totalPagado))
                .utilidad(round2(utilidad))
                .utilidadPorViaje(round2(utilidadPorViaje))
                .pagoPorViaje(round2(pagoPorViaje))
                .completamenteLiquidado(completamenteLiquidado)
                .datosPorDia(datosPorDia);

        return builder.build();
    }

    private Map<String, Optional<YangoIncomeSummary>> fetchIncomeSummariesEnParalelo(
            Map<String, List<CalculatedShift>> turnosPorConductor,
            String dateFromIso, String dateToIso) {

        if (turnosPorConductor.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Optional<YangoIncomeSummary>> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String driverId : turnosPorConductor.keySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    Optional<YangoIncomeSummary> income = fetchIncomeSummariesExacto(driverId, dateFromIso, dateToIso);
                    results.put(driverId, income);
                } catch (Exception e) {
                    log.warn("[CalculatedShiftService] error fetching income for driverId={}: {}", driverId, e.getMessage());
                    results.put(driverId, Optional.empty());
                }
            }, executorService));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(INCOME_SUMMARY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[CalculatedShiftService] timeout/error fetching income summaries: {}", e.getMessage());
        }

        return new HashMap<>(results);
    }

    private Map<String, Optional<YangoIncomeSummary>> fetchIncomeSummariesExacto(
            Map<String, List<CalculatedShift>> turnosPorConductor,
            String fechaInicio, String fechaFin) {
        String dateFrom = fechaInicio + "T00:00:00-05:00";
        String dateTo = fechaFin + "T23:59:59-05:00";
        Map<String, Optional<YangoIncomeSummary>> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String driverId : turnosPorConductor.keySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    Optional<YangoIncomeSummary> income = fetchIncomeSummariesExacto(driverId, dateFrom, dateTo);
                    results.put(driverId, income);
                } catch (Exception e) {
                    results.put(driverId, Optional.empty());
                }
            }, executorService));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(INCOME_SUMMARY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[CalculatedShiftService] timeout exacto: {}", e.getMessage());
        }

        return new HashMap<>(results);
    }

    private Optional<YangoIncomeSummary> fetchIncomeSummariesExacto(
            String driverId, String dateFrom, String dateTo) {
        try {
            return yangoWeeklyService.fetchWeeklyIncomeSummary(
                driverId, null, dateFrom, dateTo);
        } catch (Exception e) {
            log.warn("[CalculatedShiftService] error fetchIncomeSummariesExacto driverId={}: {}", driverId, e.getMessage());
            return Optional.empty();
        }
    }

    private List<ResumenSemanalResponse.DiaSemanalInfo> construirDatosPorDia(
            List<CalculatedShift> turnos, Map<String, DriverClose> cierresPorDriver) {
        Map<LocalDate, List<CalculatedShift>> turnosPorDia = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getFecha));

        return turnosPorDia.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                LocalDate fecha = entry.getKey();
                List<CalculatedShift> turnosDia = entry.getValue();
                String claveCierre = turnosDia.get(0).getDriverId() + "|" + fecha.toString();
                DriverClose cierre = cierresPorDriver.get(claveCierre);
                boolean liquidado = cierre != null;

                int cantidadViajes = turnosDia.stream().mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0).sum();
                double produccionTotal = turnosDia.stream().mapToDouble(t -> t.getProduccionTotal() != null ? t.getProduccionTotal() : 0).sum();
                double comisionesServicio = turnosDia.stream().mapToDouble(t -> t.getComisionesServicio() != null ? t.getComisionesServicio() : 0).sum();
                String turnosTipo = turnosDia.stream()
                    .map(t -> t.getTipoTurno().name().equals("manana") ? "D" : "N")
                    .distinct()
                    .collect(Collectors.joining(", "));

                return ResumenSemanalResponse.DiaSemanalInfo.builder()
                    .fecha(fecha.format(DATE_FORMATTER))
                    .diaSemana(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "PE")))
                    .cantidadViajes(cantidadViajes)
                    .cantidadTurnos(turnosDia.size())
                    .turnosTipo(turnosTipo)
                    .produccionTotal(round2(produccionTotal))
                    .comisionesServicio(round2(comisionesServicio))
                    .montoTotalPagar(round2(turnosDia.stream().mapToDouble(t -> t.getMontoTotal() != null ? t.getMontoTotal() : 0).sum()))
                    .montoTotalPagado(round2(turnosDia.stream().filter(t -> Boolean.TRUE.equals(t.getPagado())).mapToDouble(t -> t.getMontoTotal() != null ? t.getMontoTotal() : 0).sum()))
                    .gastoCombustible(obtenerGastoCombustible(cierre))
                    .liquidaEfectivo(cierre != null && cierre.getLiquidaEfectivo() != null ? cierre.getLiquidaEfectivo().doubleValue() : 0)
                    .liquidaYape(cierre != null && cierre.getLiquidaYape() != null ? cierre.getLiquidaYape().doubleValue() : 0)
                    .otrosGastos(cierre != null && cierre.getOtrosGastos() != null ? cierre.getOtrosGastos().doubleValue() : 0)
                    .odometroInicial(cierre != null ? cierre.getOdometroInicial() : null)
                    .odometroFinal(cierre != null ? cierre.getOdometroFinal() : null)
                    .kmRecorrido(calcularKmDia(cierre))
                    .liquidado(liquidado)
                    .build();
            })
            .collect(Collectors.toList());
    }

    private Map<String, DriverClose> obtenerCierresEnRango(LocalDate inicio, LocalDate fin) {
        try {
            List<DriverClose> cierres = driverCloseRepository.findByFechaBetween(inicio, fin);
            if (cierres == null || cierres.isEmpty()) return new HashMap<>();
            return cierres.stream()
                .collect(Collectors.toMap(
                    c -> c.getDriverId() + "|" + c.getFecha().toString(),
                    c -> c,
                    (a, b) -> a));
        } catch (Exception e) {
            log.warn("[CalculatedShiftService] error obteniendo cierres: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private PaidShiftsResponse construirPaidShiftsResponse(List<CalculatedShift> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return PaidShiftsResponse.builder()
                .totalConductores(0)
                .conductores(new ArrayList<>())
                .build();
        }

        Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap = obtenerMapaConductores();
        Map<String, List<CalculatedShift>> turnosPorConductor = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getDriverId));

        List<PaidShiftsResponse.ConductorTurnosPagadosInfo> conductores = new ArrayList<>();
        for (Map.Entry<String, List<CalculatedShift>> entry : turnosPorConductor.entrySet()) {
            String driverId = entry.getKey();
            List<CalculatedShift> turnosConductor = entry.getValue();
            DriverSimpleResponse.DriverInfo info = driverInfoMap.get(driverId);

            int cantidadViajes = turnosConductor.stream().mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0).sum();
            int duracionTotalMin = turnosConductor.stream().mapToInt(t -> t.getDuracionMinutos() != null ? t.getDuracionMinutos() : 0).sum();
            double viajesPorHora = duracionTotalMin > 0
                ? round2((double) cantidadViajes / (duracionTotalMin / 60.0))
                : 0;
            double produccionTotal = turnosConductor.stream().mapToDouble(t -> t.getProduccionTotal() != null ? t.getProduccionTotal() : 0).sum();
            double comisionesServicio = turnosConductor.stream().mapToDouble(t -> t.getComisionesServicio() != null ? t.getComisionesServicio() : 0).sum();
            double montoTotalPagado = turnosConductor.stream().mapToDouble(t -> t.getMontoTotal() != null ? t.getMontoTotal() : 0).sum();

            List<PaidShiftsResponse.TurnoPagadoInfo> turnosInfo = turnosConductor.stream()
                .map(t -> PaidShiftsResponse.TurnoPagadoInfo.builder()
                    .id(t.getId())
                    .fecha(t.getFecha().format(DATE_FORMATTER))
                    .horaInicio(t.getHoraInicio() != null ? t.getHoraInicio().format(DATETIME_FORMATTER) : null)
                    .horaFin(t.getHoraFin() != null ? t.getHoraFin().format(DATETIME_FORMATTER) : null)
                    .tipoTurno(t.getTipoTurno().name())
                    .duracionMinutos(t.getDuracionMinutos())
                    .montoTotal(t.getMontoTotal())
                    .pagado(t.getPagado())
                    .build())
                .collect(Collectors.toList());

            conductores.add(PaidShiftsResponse.ConductorTurnosPagadosInfo.builder()
                .driverId(driverId)
                .avatarUrl(info != null ? info.getAvatarUrl() : null)
                .nombre(info != null ? info.getNombre() : null)
                .telefono(info != null ? info.getTelefono() : null)
                .cantidadTurnos(turnosConductor.size())
                .cantidadViajes(cantidadViajes)
                .viajesPorHora(viajesPorHora)
                .montoTotalPagado(round2(montoTotalPagado))
                .produccionTotal(round2(produccionTotal))
                .comisionesServicio(round2(comisionesServicio))
                .turnos(turnosInfo)
                .build());
        }

        return PaidShiftsResponse.builder()
            .totalConductores(conductores.size())
            .conductores(conductores)
            .build();
    }

    @Override
    public FacturacionSemanal registrarFacturacionSemanal(FacturacionSemanal facturacion) {
        Optional<FacturacionSemanal> existente = facturacionSemanalRepository
            .findByDriverIdAndFechaInicioAndFechaFin(
                facturacion.getDriverId(),
                facturacion.getFechaInicio(),
                facturacion.getFechaFin());

        if (existente.isPresent()) {
            FacturacionSemanal actual = existente.get();
            actual.setTotalViajes(facturacion.getTotalViajes());
            actual.setViajesValidos(facturacion.getViajesValidos());
            actual.setHorasTrabajo(facturacion.getHorasTrabajo());
            actual.setMontoTotalProducido(facturacion.getMontoTotalProducido());
            actual.setComisionApp(facturacion.getComisionApp());
            actual.setMontoNeto(facturacion.getMontoNeto());
            actual.setKmRecorrido(facturacion.getKmRecorrido());
            actual.setGastoCombustible(facturacion.getGastoCombustible());
            actual.setBonoYango(facturacion.getBonoYango());
            actual.setGastoMantenimiento(facturacion.getGastoMantenimiento());
            actual.setProduccionBonificable(facturacion.getProduccionBonificable());
            actual.setBonoAdicViajes(facturacion.getBonoAdicViajes());
            actual.setBono(facturacion.getBono());
            actual.setPorcentajePago(facturacion.getPorcentajePago());
            actual.setPago(facturacion.getPago());
            actual.setBonificacion(facturacion.getBonificacion());
            actual.setGarantia(facturacion.getGarantia());
            actual.setDescuento(facturacion.getDescuento());
            actual.setGeneral(facturacion.getGeneral());
            actual.setPagoTotal(facturacion.getPagoTotal());
            actual.setUtilidad(facturacion.getUtilidad());
            actual.setUtilidadPorViaje(facturacion.getUtilidadPorViaje());
            actual.setPagoPorViaje(facturacion.getPagoPorViaje());
            actual.setDiasTrabajados(facturacion.getDiasTrabajados());
            actual.setDiasLiquidados(facturacion.getDiasLiquidados());
            actual.setTurno(facturacion.getTurno());
            actual.setEstado(facturacion.getEstado() != null ? facturacion.getEstado() : "pendiente");
            actual.setUserId(facturacion.getUserId());
            return facturacionSemanalRepository.save(actual);
        }

        return facturacionSemanalRepository.save(facturacion);
    }

    @Override
    public List<FacturacionSemanal> obtenerHistorialFacturacion(String fechaInicio, String fechaFin) {
        if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
            LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
            LocalDate fin = LocalDate.parse(fechaFin, DATE_FORMATTER);
            return facturacionSemanalRepository.findByRangoFechas(inicio, fin);
        }
        if (fechaInicio != null && !fechaInicio.isEmpty()) {
            LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
            return facturacionSemanalRepository.findByRangoFechas(inicio, LocalDate.now(LIMA_ZONE));
        }
        return facturacionSemanalRepository.findAllByOrderByFechaInicioDesc();
    }

    @Override
    public BillingConfigResponse obtenerConfiguracionBilling() {
        List<BonusThreshold> bonusThresholds = bonusThresholdRepository.findAll();
        List<PaymentPercentage> paymentPercentages = paymentPercentageRepository.findAll();
        return BillingConfigResponse.builder()
            .bonusThresholds(bonusThresholds)
            .paymentPercentages(paymentPercentages)
            .build();
    }

    @Override
    public BillingConfigResponse guardarConfiguracionBilling(BillingConfigResponse config, Long userId) {
        if (config.getBonusThresholds() != null) {
            bonusThresholdRepository.deleteAll();
            bonusThresholdRepository.flush();
            for (BonusThreshold bt : config.getBonusThresholds()) {
                bt.setId(null);
                bt.setUpdatedBy(userId);
                bonusThresholdRepository.save(bt);
            }
        }
        if (config.getPaymentPercentages() != null) {
            paymentPercentageRepository.deleteAll();
            paymentPercentageRepository.flush();
            for (PaymentPercentage pp : config.getPaymentPercentages()) {
                pp.setId(null);
                pp.setUpdatedBy(userId);
                paymentPercentageRepository.save(pp);
            }
        }
        return obtenerConfiguracionBilling();
    }

    @Override
    public byte[] exportarAsistenciaExcel(String fechaInicio, String fechaFin) {
        LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
        LocalDate fin = LocalDate.parse(fechaFin, DATE_FORMATTER);

        List<FacturacionSemanal> facturaciones = facturacionSemanalRepository.findByFechaInicioAndFechaFin(inicio, fin);
        if (facturaciones != null && !facturaciones.isEmpty()) {
            return generarExcelAsistenciaDesdeBD(facturaciones, inicio, fin, fechaInicio, fechaFin);
        }

        List<CalculatedShift> turnos = calculatedShiftRepository.findByFechaBetween(inicio, fin);
        if (turnos.isEmpty()) return new byte[0];

        ResumenSemanalResponse resumen = obtenerResumenSemanal(fechaInicio, fechaFin);
        Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap = obtenerMapaConductores();
        return generarExcelAsistenciaDesdeResumen(resumen, driverInfoMap, fechaInicio, fechaFin);
    }

    private byte[] generarExcelAsistenciaDesdeBD(List<FacturacionSemanal> facturaciones,
            LocalDate inicio, LocalDate fin, String fechaInicio, String fechaFin) {
        List<CalculatedShift> turnos = calculatedShiftRepository.findByFechaBetween(inicio, fin);
        Map<String, DriverClose> cierresPorDriver = obtenerCierresEnRango(inicio, fin);
        Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap = obtenerMapaConductores();
        ResumenSemanalResponse resumen = construirResumenDesdeBD(facturaciones, turnos, cierresPorDriver);
        return generarExcelAsistenciaDesdeResumen(resumen, driverInfoMap, fechaInicio, fechaFin);
    }

    private byte[] generarExcelAsistenciaDesdeResumen(ResumenSemanalResponse resumen,
            Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap,
            String fechaInicio, String fechaFin) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Asistencia Yego PRO");
            CellStyle titleStyle = crearEstiloTitulo(workbook);
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle moneyStyle = crearEstiloMoney(workbook);
            CellStyle textStyle = crearEstiloTexto(workbook);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Asistencia Yego PRO — Semana " + fechaInicio + " al " + fechaFin);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 12));

            String[] headers = {"Conductor", "Persona que cobra", "Tipo de documento", "N° documento",
                "Banco", "CCI", "Interbancario", "Cuenta", "Monto", "Garantía", "Descuento", "Bonificación", "Total"};
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 2;
            if (resumen != null && resumen.getConductores() != null) {
                for (ResumenSemanalResponse.ConductorSemanalInfo c : resumen.getConductores()) {
                    Row row = sheet.createRow(rowIdx++);
                    DriverSimpleResponse.DriverInfo info = driverInfoMap.get(c.getDriverId());
                    double monto = c.getPagoTotal();
                    double garantia = 0;
                    double descuento = 0;
                    double bonificacion = 0;

                    setCell(row, 0, info != null ? info.getNombre() : c.getDriverId(), textStyle);
                    setCell(row, 1, "", textStyle);
                    setCell(row, 2, "", textStyle);
                    setCell(row, 3, "", textStyle);
                    setCell(row, 4, "", textStyle);
                    setCell(row, 5, "", textStyle);
                    row.createCell(6).setCellStyle(moneyStyle);
                    setCell(row, 7, "", textStyle);
                    if (monto > 0) setCellValue(row, 8, monto, moneyStyle);
                    else row.createCell(8).setCellStyle(moneyStyle);
                    if (garantia > 0) setCellValue(row, 9, garantia, moneyStyle);
                    else row.createCell(9).setCellStyle(moneyStyle);
                    if (descuento > 0) setCellValue(row, 10, descuento, moneyStyle);
                    else row.createCell(10).setCellStyle(moneyStyle);
                    if (bonificacion > 0) setCellValue(row, 11, bonificacion, moneyStyle);
                    else row.createCell(11).setCellStyle(moneyStyle);
                    double total = monto - garantia - descuento + bonificacion;
                    setCellValue(row, 12, total, moneyStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("[exportarAsistenciaExcel] error: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar Excel de asistencia", e);
        }
    }

    @Override
    public int guardarSnapshotSemanal(String fechaInicio, String fechaFin) {
        LocalDate inicio = LocalDate.parse(fechaInicio, DATE_FORMATTER);
        LocalDate fin = LocalDate.parse(fechaFin, DATE_FORMATTER);

        log.info("[guardarSnapshot] iniciando para {}/{}", fechaInicio, fechaFin);
        ResumenSemanalResponse resumen = obtenerResumenSemanal(fechaInicio, fechaFin);
        if (resumen.getConductores() == null || resumen.getConductores().isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ResumenSemanalResponse.ConductorSemanalInfo c : resumen.getConductores()) {
            if (c.getDiasTrabajados() == 0) continue;

            FacturacionSemanal entity = FacturacionSemanal.builder()
                .driverId(c.getDriverId())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .totalViajes(c.getTotalViajes())
                .viajesValidos(c.getViajesValidos())
                .horasTrabajo(c.getHorasTrabajo())
                .montoTotalProducido(BigDecimal.valueOf(c.getMontoTotalProducido()))
                .comisionApp(BigDecimal.valueOf(c.getComisionApp()))
                .montoNeto(BigDecimal.valueOf(c.getMontoNeto()))
                .kmRecorrido(BigDecimal.valueOf(c.getKmRecorrido()))
                .gastoCombustible(BigDecimal.valueOf(c.getGastoCombustible()))
                .bonoYango(BigDecimal.valueOf(c.getBonoYango()))
                .gastoMantenimiento(BigDecimal.valueOf(c.getGastoMantenimiento()))
                .produccionBonificable(BigDecimal.valueOf(c.getProduccionBonificable()))
                .bonoAdicViajes(BigDecimal.valueOf(c.getBonoAdicViajes()))
                .bono(BigDecimal.valueOf(c.getBono()))
                .porcentajePago(c.getPorcentajePago())
                .pago(BigDecimal.valueOf(c.getPago()))
                .pagoTotal(BigDecimal.valueOf(c.getPagoTotal()))
                .utilidad(BigDecimal.valueOf(c.getUtilidad()))
                .utilidadPorViaje(BigDecimal.valueOf(c.getUtilidadPorViaje()))
                .pagoPorViaje(BigDecimal.valueOf(c.getPagoPorViaje()))
                .diasTrabajados(c.getDiasTrabajados())
                .diasLiquidados(c.getDiasLiquidados())
                .turno(c.getTurno())
                .estado("pendiente")
                .build();

            registrarFacturacionSemanal(entity);
            count++;
        }

        return count;
    }

    private ResumenSemanalResponse construirResumenDesdeBD(List<FacturacionSemanal> snapshot,
            List<CalculatedShift> turnos, Map<String, DriverClose> cierresPorDriver) {
        Map<String, List<CalculatedShift>> turnosPorConductor = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getDriverId));
        Map<String, DriverSimpleResponse.DriverInfo> driverInfoMap = obtenerMapaConductores();
        List<ResumenSemanalResponse.ConductorSemanalInfo> conductores = snapshot.stream()
            .map(f -> {
                DriverSimpleResponse.DriverInfo info = driverInfoMap.get(f.getDriverId());
                List<CalculatedShift> turnosConductor = turnosPorConductor.getOrDefault(f.getDriverId(), List.of());
                List<ResumenSemanalResponse.DiaSemanalInfo> datosPorDia = construirDatosPorDia(turnosConductor, cierresPorDriver);
                return ResumenSemanalResponse.ConductorSemanalInfo.builder()
                .driverId(f.getDriverId())
                .avatarUrl(info != null ? info.getAvatarUrl() : null)
                .nombre(info != null && info.getNombre() != null ? info.getNombre() : f.getDriverId())
                .telefono(info != null ? info.getTelefono() : null)
                .placa(null)
                .diasTrabajados(f.getDiasTrabajados()).diasLiquidados(f.getDiasLiquidados())
                .totalViajes(f.getTotalViajes()).viajesValidos(f.getViajesValidos())
                .horasTrabajo(f.getHorasTrabajo())
                .tph(f.getTotalViajes() != null && f.getHorasTrabajo() != null && f.getHorasTrabajo() > 0
                    ? round2((double) f.getTotalViajes() / f.getHorasTrabajo()) : 0)
                .montoTotalProducido(toDouble(f.getMontoTotalProducido()))
                .comisionApp(toDouble(f.getComisionApp()))
                .montoNeto(toDouble(f.getMontoNeto()))
                .kmRecorrido(toDouble(f.getKmRecorrido()))
                .gastoCombustible(toDouble(f.getGastoCombustible()))
                .bonoYango(toDouble(f.getBonoYango()))
                .gastoMantenimiento(toDouble(f.getGastoMantenimiento()))
                .produccionBonificable(toDouble(f.getProduccionBonificable()))
                .bonoAdicViajes(f.getBonoAdicViajes() != null ? f.getBonoAdicViajes().intValue() : 0)
                .bono(toDouble(f.getBono()))
                .porcentajePago(f.getPorcentajePago() != null ? f.getPorcentajePago() : 0)
                .pago(toDouble(f.getPago()))
                .pagoTotal(toDouble(f.getPagoTotal()))
                .totalPagado(0)
                .utilidad(toDouble(f.getUtilidad()))
                .utilidadPorViaje(toDouble(f.getUtilidadPorViaje()))
                .pagoPorViaje(toDouble(f.getPagoPorViaje()))
                .turno(f.getTurno())
                .completamenteLiquidado(true)
                .datosPorDia(datosPorDia)
                .build();
            })
            .collect(Collectors.toList());

        double totalProduccion = conductores.stream().mapToDouble(c -> c.getMontoTotalProducido()).sum();
        double totalComision = conductores.stream().mapToDouble(c -> c.getComisionApp()).sum();
        double totalCombustible = conductores.stream().mapToDouble(c -> c.getGastoCombustible()).sum();
        double totalPagar = conductores.stream().mapToDouble(c -> c.getPagoTotal()).sum();
        double totalPagado = conductores.stream().mapToDouble(c -> c.getTotalPagado()).sum();
        double totalBonos = conductores.stream().mapToDouble(c -> c.getBono()).sum();
        double totalUtilidad = conductores.stream().mapToDouble(c -> c.getUtilidad()).sum();
        int totalTurnos = turnos.size();
        int totalViajes = turnos.stream().mapToInt(t -> t.getCantidadViajes() != null ? t.getCantidadViajes() : 0).sum();

        return ResumenSemanalResponse.builder()
            .fechaInicio(snapshot.get(0).getFechaInicio().toString())
            .fechaFin(snapshot.get(0).getFechaFin().toString())
            .totalConductores(conductores.size())
            .totalViajes(totalViajes).totalProduccion(round2(totalProduccion))
            .totalComision(round2(totalComision)).totalCombustible(round2(totalCombustible))
            .totalPagar(round2(totalPagar)).totalPagado(round2(totalPagado))
            .totalPendiente(round2(Math.max(0, totalPagar - totalPagado)))
            .totalBonos(round2(totalBonos)).totalUtilidad(round2(totalUtilidad))
            .totalTurnos(totalTurnos)
            .conductores(conductores)
            .build();
    }

    private Map<String, DriverSimpleResponse.DriverInfo> obtenerMapaConductores() {
        try {
            DriverSimpleResponse driverList = fleetDriverService.obtenerListaConductoresSimplificada();
            if (driverList == null || driverList.getConductores() == null) {
                return new HashMap<>();
            }
            return driverList.getConductores().stream()
                .filter(d -> d.getDriverId() != null)
                .collect(Collectors.toMap(
                    DriverSimpleResponse.DriverInfo::getDriverId,
                    d -> d,
                    (a, b) -> a));
        } catch (Exception e) {
            log.warn("[CalculatedShiftService] error obteniendo lista conductores: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }

    private static double obtenerGastoCombustible(DriverClose cierre) {
        if (cierre == null) return 0;
        double gasto = 0;
        if (cierre.getGnvSoles() != null) gasto += cierre.getGnvSoles().doubleValue();
        if (cierre.getGasolinaSoles() != null) gasto += cierre.getGasolinaSoles().doubleValue();
        return gasto;
    }

    private static double calcularKmDia(DriverClose cierre) {
        if (cierre == null) return 0;
        Integer ini = cierre.getOdometroInicial();
        Integer fin = cierre.getOdometroFinal();
        if (ini != null && fin != null && fin > ini) return fin - ini;
        if (cierre.getDiferenciaOdometro() != null) return cierre.getDiferenciaOdometro().doubleValue();
        return 0;
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle crearEstiloMoney(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloTexto(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null && !value.isEmpty()) cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCellValue(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCellValue(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCellValue(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private record Cached<T>(T value, long expiresAt) {
        static <T> Cached<T> of(T value, long ttlMs) {
            return new Cached<>(value, System.currentTimeMillis() + ttlMs);
        }
        boolean expired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    private static class ViajeTemporal {
        final OrderInfoResponse order;
        final LocalDateTime endedAt;

        ViajeTemporal(OrderInfoResponse order, LocalDateTime endedAt) {
            this.order = order;
            this.endedAt = endedAt;
        }
    }
}
