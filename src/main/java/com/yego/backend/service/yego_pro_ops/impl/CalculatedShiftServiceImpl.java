package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.repository.yego_pro_ops.CalculatedShiftRepository;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.FleetDriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatedShiftServiceImpl implements CalculatedShiftService {

    // ==================== CONSTANTS ====================
    
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DATETIME_MANUAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATETIME_MANUAL_FORMATTER_NO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FECHA_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    private static final int HORA_INICIO_DIURNO = 5;
    private static final int HORA_FIN_DIURNO = 18;
    private static final int HORA_INICIO_NOCTURNO = 18;
    private static final int HORA_FIN_NOCTURNO = 5;
    private static final int HORA_MEDIANOCHE = 0;
    private static final int MINUTO_FIN_DIA = 59;
    private static final int SEGUNDO_FIN_DIA = 59;
    
    private static final long DELAY_ENTRE_CONDUCTORES_MS = 2000; // 2 segundos
    private static final int LOG_PROGRESO_CADA_N = 5; // Log cada 5 conductores
    private static final long INACTIVIDAD_MAXIMA_HORAS = 5; // 5 horas de inactividad para cerrar el turno
    private static final long INACTIVIDAD_MAXIMA_MINUTOS = INACTIVIDAD_MAXIMA_HORAS * 60; // 300 minutos
    
    // ==================== FIELDS ====================
    
    private final CalculatedShiftRepository calculatedShiftRepository;
    private final DriverOrdersService driverOrdersService;
    private final FleetDriverService fleetDriverService;
    
    // ==================== INITIALIZATION ====================
    
    {
        log.info("✅ [CalculatedShiftService] Inicializado correctamente - Zona horaria: {}, Dependencias: DriverOrdersService, FleetDriverService", 
            LIMA_ZONE);
    }
    
    // ==================== PUBLIC METHODS ====================
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calcularYGuardarHorasTurno(String driverId, LocalDate fecha) {
        try {
            if (!validarParametros(driverId, fecha)) {
                return;
            }
            
            log.info("🕐 [CalculatedShiftService] Calculando horas de turno para driver_id: {}, fecha: {}", driverId, fecha);
            
            ViajesDelDia viajesDelDia = obtenerViajesDelDia(driverId, fecha);
            
            // Obtener primero los viajes nocturnos del día actual para decidir si consultar el día anterior
            LocalDate fechaSiguiente = fecha.plusDays(1);
            List<OrderInfoResponse> viajesNocturnosActual = obtenerViajesNocturnosRango(
                driverId, fecha, fechaSiguiente, "día actual");
            
            // Decidir si consultar el día anterior basándose en viajes del día y viajes nocturnos del día actual
            boolean debeConsultarDiaAnterior = debeConsultarDiaAnterior(viajesDelDia, viajesNocturnosActual);
            
            // Obtener los viajes nocturnos del día anterior si es necesario
            LocalDate fechaAnterior = fecha.minusDays(1);
            List<OrderInfoResponse> viajesNocturnosAnterior = debeConsultarDiaAnterior
                ? obtenerViajesNocturnosRango(driverId, fechaAnterior, fecha, "día anterior")
                : new ArrayList<>();
            
            ViajesNocturnos viajesNocturnos = new ViajesNocturnos(
                viajesNocturnosAnterior, viajesNocturnosActual, fechaAnterior, fechaSiguiente);
            
            procesarViajesMadrugada(viajesDelDia, viajesNocturnos, driverId, fecha);
            logResumenViajes(driverId, fecha, viajesDelDia, viajesNocturnos);
            guardarTurnos(driverId, fecha, viajesDelDia, viajesNocturnos);
            
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error calculando horas de turno para driver_id: {}, fecha: {}: {}", 
                driverId, fecha, e.getMessage(), e);
        }
    }
    
    private boolean debeConsultarDiaAnterior(ViajesDelDia viajesDelDia, List<OrderInfoResponse> viajesNocturnosActual) {
        // Obtener el primer viaje del día (sin importar si es madrugada o diurno)
        LocalDateTime primerViajeDelDia = obtenerPrimerViajeDelDia(viajesDelDia);
        
        // Obtener el primer viaje nocturno del día actual
        LocalDateTime primerViajeNocturnoActual = viajesNocturnosActual.isEmpty()
            ? null
            : obtenerPrimerViaje(viajesNocturnosActual);
        
        // Si no hay viajes del día pero hay viajes nocturnos del día actual, verificar si son tempranos
        if (primerViajeDelDia == null) {
            if (primerViajeNocturnoActual != null) {
                int horaPrimerViajeNocturno = primerViajeNocturnoActual.getHour();
                // Si el primer viaje nocturno es antes de las 6 AM, consultar el día anterior
                if (horaPrimerViajeNocturno < HORA_INICIO_DIURNO) {
                    log.info("📅 [CalculatedShiftService] No hay viajes del día, pero hay viajes nocturnos a las {} (antes de las 6 AM). Se consultará el día anterior.", 
                        primerViajeNocturnoActual.toLocalTime());
                    return true;
                }
            }
            log.info("📅 [CalculatedShiftService] No hay viajes del día. No se consultará el día anterior.");
            return false;
        }
        
        int horaPrimerViaje = primerViajeDelDia.getHour();
        
        // Si el primer viaje del día es a las 00, 1, 2, 3, 4, o 5 AM, consultar el día anterior
        if (horaPrimerViaje < HORA_INICIO_DIURNO) {
            log.info("📅 [CalculatedShiftService] Primer viaje del día es a las {} (antes de las 6 AM). Se consultará el día anterior.", 
                primerViajeDelDia.toLocalTime());
            return true;
        }
        
        // Si el primer viaje es después de las 5 AM, no consultar el día anterior
        log.info("📅 [CalculatedShiftService] Primer viaje del día es a las {} (después de las 5 AM). No se consultará el día anterior.", 
            primerViajeDelDia.toLocalTime());
        return false;
    }
    
    private LocalDateTime obtenerPrimerViajeDelDia(ViajesDelDia viajesDelDia) {
        LocalDateTime primerViajeMadrugada = viajesDelDia.viajesMadrugada.isEmpty() 
            ? null 
            : obtenerPrimerViaje(viajesDelDia.viajesMadrugada);
        
        LocalDateTime primerViajeDiurno = viajesDelDia.viajesDiurnos.isEmpty() 
            ? null 
            : obtenerPrimerViaje(viajesDelDia.viajesDiurnos);
        
        // Retornar el más temprano entre madrugada y diurno
        if (primerViajeMadrugada == null) {
            return primerViajeDiurno;
        }
        if (primerViajeDiurno == null) {
            return primerViajeMadrugada;
        }
        
        return primerViajeMadrugada.isBefore(primerViajeDiurno) 
            ? primerViajeMadrugada 
            : primerViajeDiurno;
    }
    
    @Override
    public void procesarHorasTurnoDiaAnterior() {
        LocalDate fechaActual = LocalDate.now(LIMA_ZONE);
        LocalDate fechaAnterior = fechaActual.minusDays(1);
        
        logInicioProcesamiento(fechaActual, fechaAnterior);
        
        try {
            var listaConductores = obtenerListaConductores();
            if (listaConductores == null || listaConductores.getContractors() == null || listaConductores.getContractors().isEmpty()) {
                log.warn("⚠️ [CalculatedShiftService] No se encontraron conductores para procesar");
                return;
            }
            
            EstadisticasProcesamiento stats = procesarConductores(listaConductores.getContractors(), fechaAnterior);
            logFinProcesamiento(fechaAnterior, stats);
            
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error crítico en procesamiento de horas de turno del día anterior (fecha: {}): {}", 
                fechaAnterior, e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId) {
        log.info("📅 [CalculatedShiftService] Obteniendo fechas con tipos de turno para driver_id: {}", driverId);
        
        try {
            List<CalculatedShift> shifts = calculatedShiftRepository.findByDriverIdOrderByFecha(driverId);
            
            if (shifts.isEmpty()) {
                log.info("ℹ️ [CalculatedShiftService] No se encontraron turnos para driver_id: {}", driverId);
                return crearRespuestaVacia(driverId);
            }
            
            List<FechasConTiposTurnoResponse.FechaConTiposTurno> fechasConTipos = agruparShiftsPorFecha(shifts);
            
            log.info("✅ [CalculatedShiftService] Se encontraron {} fechas únicas para driver_id: {}", 
                fechasConTipos.size(), driverId);
            
            return FechasConTiposTurnoResponse.builder()
                .driverId(driverId)
                .fechas(fechasConTipos)
                .build();
                    
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error obteniendo fechas con tipos de turno para driver_id {}: {}", 
                driverId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener fechas con tipos de turno: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public DriverPaymentSummaryResponse obtenerResumenPagos(String fecha) {
        log.info("💰 [CalculatedShiftService] Obteniendo resumen de pagos de todos los conductores para fecha: {}", fecha);
        
        try {
            LocalDate fechaLocal = parsearFecha(fecha);
            if (fechaLocal == null) {
                log.error("❌ [CalculatedShiftService] Fecha inválida: {}", fecha);
                throw new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD");
            }
            
            List<CalculatedShift> turnosPorFecha = obtenerTurnosPorFecha(fechaLocal);
            if (turnosPorFecha.isEmpty()) {
                log.info("ℹ️ [CalculatedShiftService] No se encontraron turnos para la fecha: {}", fechaLocal);
                return crearRespuestaVaciaResumenPagos();
            }
            
            Map<String, List<CalculatedShift>> turnosPorConductor = agruparTurnosPorConductor(turnosPorFecha);
            Map<String, ConductorInfo> infoConductores = obtenerInfoConductores();
            
            List<DriverPaymentSummaryResponse.ConductorPaymentInfo> conductoresInfo = 
                construirInfoConductores(turnosPorConductor, infoConductores);
            
            log.info("✅ [CalculatedShiftService] Resumen de pagos generado para {} conductores en fecha {}", 
                conductoresInfo.size(), fechaLocal);
            
            return DriverPaymentSummaryResponse.builder()
                .conductores(conductoresInfo)
                .build();
                    
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error obteniendo resumen de pagos para fecha {}: {}", 
                fecha, e.getMessage(), e);
            throw new RuntimeException("Error al obtener resumen de pagos: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PaidShiftsResponse obtenerTurnosPagados(String fecha) {
        log.info("💰 [CalculatedShiftService] Obteniendo turnos pagados{}", 
            fecha != null ? " para fecha: " + fecha : "");
        
        try {
            LocalDate fechaLocal = fecha != null ? parsearFecha(fecha) : null;
            if (fecha != null && fechaLocal == null) {
                log.error("❌ [CalculatedShiftService] Fecha inválida: {}", fecha);
                throw new IllegalArgumentException("Fecha inválida. Formato esperado: YYYY-MM-DD");
            }
            
            List<CalculatedShift> turnosPagados = fechaLocal != null 
                ? calculatedShiftRepository.findByPagadoTrueAndFecha(fechaLocal)
                : calculatedShiftRepository.findByPagadoTrue();
            
            if (turnosPagados.isEmpty()) {
                log.info("ℹ️ [CalculatedShiftService] No se encontraron turnos pagados{}", 
                    fechaLocal != null ? " para la fecha: " + fechaLocal : "");
                return PaidShiftsResponse.builder()
                    .totalConductores(0)
                    .conductores(new ArrayList<>())
                    .build();
            }
            
            // Agrupar turnos por conductor
            Map<String, List<CalculatedShift>> turnosPorConductor = turnosPagados.stream()
                .collect(Collectors.groupingBy(CalculatedShift::getDriverId));
            
            // Obtener información de los conductores
            Map<String, ConductorInfo> infoConductores = obtenerInfoConductores();
            
            // Construir la respuesta agrupada por conductor
            List<PaidShiftsResponse.ConductorTurnosPagadosInfo> conductoresInfo = 
                construirInfoConductoresTurnosPagados(turnosPorConductor, infoConductores);
            
            log.info("✅ [CalculatedShiftService] Se encontraron {} turnos pagados agrupados en {} conductores{}", 
                turnosPagados.size(), conductoresInfo.size(), fechaLocal != null ? " para fecha: " + fechaLocal : "");
            
            return PaidShiftsResponse.builder()
                .totalConductores(conductoresInfo.size())
                .conductores(conductoresInfo)
                .build();
                    
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error obteniendo turnos pagados{}: {}", 
                fecha != null ? " para fecha " + fecha : "", e.getMessage(), e);
            throw new RuntimeException("Error al obtener turnos pagados: " + e.getMessage(), e);
        }
    }
    
    private List<PaidShiftsResponse.ConductorTurnosPagadosInfo> construirInfoConductoresTurnosPagados(
            Map<String, List<CalculatedShift>> turnosPorConductor,
            Map<String, ConductorInfo> infoConductores) {
        
        return turnosPorConductor.entrySet().stream()
            .map(entry -> construirInfoConductorTurnosPagados(entry.getKey(), entry.getValue(), infoConductores))
            .collect(Collectors.toList());
    }
    
    private PaidShiftsResponse.ConductorTurnosPagadosInfo construirInfoConductorTurnosPagados(
            String driverId,
            List<CalculatedShift> turnos,
            Map<String, ConductorInfo> infoConductores) {
        
        ConductorInfo info = infoConductores.getOrDefault(driverId, new ConductorInfo());
        List<PaidShiftsResponse.TurnoPagadoInfo> turnosInfo = turnos.stream()
            .map(this::convertirATurnoPagadoInfo)
            .collect(Collectors.toList());
        
        // Calcular monto total pagado
        double montoTotalPagado = turnos.stream()
            .mapToDouble(turno -> turno.getMontoTotal() != null ? turno.getMontoTotal() : 0.0)
            .sum();
        montoTotalPagado = Math.round(montoTotalPagado * 100.0) / 100.0;
        
        return PaidShiftsResponse.ConductorTurnosPagadosInfo.builder()
            .driverId(driverId)
            .avatarUrl(info.avatarUrl)
            .nombre(info.nombre)
            .telefono(info.telefono)
            .cantidadTurnos(turnos.size())
            .montoTotalPagado(montoTotalPagado)
            .turnos(turnosInfo)
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
            .pagado(turno.getPagado() != null ? turno.getPagado() : false)
            .build();
    }
    
    // ==================== PRIVATE METHODS: PAYMENT SUMMARY ====================
    
    private LocalDate parsearFecha(String fecha) {
        try {
            return LocalDate.parse(fecha, DATE_FORMATTER);
        } catch (Exception e) {
            log.debug("Error parseando fecha: {}", fecha);
            return null;
        }
    }
    
    private List<CalculatedShift> obtenerTurnosPorFecha(LocalDate fecha) {
        return calculatedShiftRepository.findByFechaOrderByDriverId(fecha);
    }
    
    private DriverPaymentSummaryResponse crearRespuestaVaciaResumenPagos() {
        log.info("ℹ️ [CalculatedShiftService] No se encontraron turnos");
        return DriverPaymentSummaryResponse.builder()
            .conductores(new ArrayList<>())
            .build();
    }
    
    private Map<String, List<CalculatedShift>> agruparTurnosPorConductor(List<CalculatedShift> turnos) {
        Map<String, List<CalculatedShift>> turnosPorConductor = turnos.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getDriverId));
        log.info("📊 [CalculatedShiftService] Se encontraron {} conductores con turnos", turnosPorConductor.size());
        return turnosPorConductor;
    }
    
    private static class ConductorInfo {
        String avatarUrl;
        String nombre;
        String telefono;
    }
    
    private Map<String, ConductorInfo> obtenerInfoConductores() {
        DriverListResponse listaConductores = fleetDriverService.obtenerListaConductores(null);
        Map<String, ConductorInfo> infoMap = new java.util.HashMap<>();
        
        if (listaConductores != null && listaConductores.getContractors() != null) {
            listaConductores.getContractors().forEach(contractor -> {
                if (contractor.getId() != null) {
                    ConductorInfo info = new ConductorInfo();
                    info.avatarUrl = contractor.getAvatarUrl();
                    info.nombre = contractor.getFullName();
                    info.telefono = contractor.getPhone();
                    infoMap.put(contractor.getId(), info);
                }
            });
        }
        
        return infoMap;
    }
    
    private List<DriverPaymentSummaryResponse.ConductorPaymentInfo> construirInfoConductores(
            Map<String, List<CalculatedShift>> turnosPorConductor,
            Map<String, ConductorInfo> infoConductores) {
        
        return turnosPorConductor.entrySet().stream()
            .map(entry -> construirInfoConductor(entry.getKey(), entry.getValue(), infoConductores))
            .collect(Collectors.toList());
    }
    
    private DriverPaymentSummaryResponse.ConductorPaymentInfo construirInfoConductor(
            String driverId,
            List<CalculatedShift> turnos,
            Map<String, ConductorInfo> infoConductores) {
        
        Double montoTotalPagar = calcularMontoTotalPagar(turnos);
        ConductorInfo info = infoConductores.getOrDefault(driverId, new ConductorInfo());
        List<DriverPaymentSummaryResponse.TurnoInfo> turnosInfo = mapearTurnosAInfo(turnos);
        
        return DriverPaymentSummaryResponse.ConductorPaymentInfo.builder()
            .driverId(driverId)
            .avatarUrl(info.avatarUrl)
            .nombre(info.nombre)
            .telefono(info.telefono)
            .montoTotalPagar(montoTotalPagar)
            .cantidadTurnos(turnos.size())
            .turnos(turnosInfo)
            .build();
    }
    
    private Double calcularMontoTotalPagar(List<CalculatedShift> turnos) {
        double montoTotal = turnos.stream()
            .filter(turno -> turno.getPagado() == null || !turno.getPagado())
            .mapToDouble(turno -> turno.getMontoTotal() != null ? turno.getMontoTotal() : 0.0)
            .sum();
        
        return Math.round(montoTotal * 100.0) / 100.0;
    }
    
    private List<DriverPaymentSummaryResponse.TurnoInfo> mapearTurnosAInfo(List<CalculatedShift> turnos) {
        return turnos.stream()
            .map(this::mapearTurnoAInfo)
            .collect(Collectors.toList());
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
            .pagado(turno.getPagado() != null ? turno.getPagado() : false)
            .build();
    }
    
    // ==================== PRIVATE METHODS: VALIDATION ====================
    
    private boolean validarParametros(String driverId, LocalDate fecha) {
        if (fecha == null) {
            log.error("❌ [CalculatedShiftService] Fecha es null para driver_id: {}", driverId);
            return false;
        }
        return true;
    }
    
    // ==================== PRIVATE METHODS: DATE PARSING ====================
    
    private LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, ORDER_DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    
    private LocalDateTime obtenerFechaViaje(OrderInfoResponse order) {
        if (order.getEndedAt() != null && !order.getEndedAt().isEmpty()) {
            LocalDateTime fecha = parseDate(order.getEndedAt());
            if (fecha != null) return fecha;
        }
        return order.getBookedAt() != null && !order.getBookedAt().isEmpty() 
            ? parseDate(order.getBookedAt()) 
            : null;
    }
    
    private ZonedDateTime parsearFechaViaje(String fechaStr) {
        try {
            return ZonedDateTime.parse(fechaStr);
        } catch (Exception e) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(fechaStr, ORDER_DATE_FORMATTER);
                return localDateTime.atZone(LIMA_ZONE);
            } catch (Exception e2) {
                try {
                    LocalDateTime localDateTime = LocalDateTime.parse(fechaStr, DATE_FORMATTER_WITH_MILLIS);
                    return localDateTime.atZone(LIMA_ZONE);
                } catch (Exception e3) {
                    log.error("❌ [CalculatedShiftService] Error parseando fecha: {}", fechaStr);
                    throw new RuntimeException("Error parseando fecha: " + fechaStr, e3);
                }
            }
        }
    }
    
    // ==================== PRIVATE METHODS: TRIP HELPERS ====================
    
    private LocalDateTime obtenerPrimerViaje(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .map(this::obtenerFechaViaje)
            .filter(java.util.Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }
    
    private LocalDateTime obtenerUltimoViaje(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .map(viaje -> {
                if (viaje.getEndedAt() != null && !viaje.getEndedAt().isEmpty()) {
                    try {
                        ZonedDateTime endedAt = parsearFechaViaje(viaje.getEndedAt());
                        return endedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            })
            .filter(java.util.Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }
    
    private LocalDateTime obtenerUltimoFinViaje(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .map(this::obtenerFechaFinViaje)
            .filter(java.util.Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }
    
    // ==================== PRIVATE METHODS: NIGHT SHIFT VERIFICATION ====================
    
    private boolean esTurnoNocturnoDelDiaAnterior(String driverId, LocalDateTime fechaViaje) {
        if (fechaViaje.getHour() >= HORA_INICIO_DIURNO) {
            return false;
        }
        
        LocalDate diaAnterior = fechaViaje.toLocalDate().minusDays(1);
        
        try {
            // Consultar desde 1 hora antes de las 18:00 (17:00) para capturar viajes tempranos
            ZonedDateTime inicioNocturno = diaAnterior.atTime(HORA_INICIO_NOCTURNO - 1, 0, 0).atZone(LIMA_ZONE);
            ZonedDateTime finNocturno = fechaViaje.toLocalDate().atTime(HORA_FIN_NOCTURNO - 1, MINUTO_FIN_DIA, SEGUNDO_FIN_DIA).atZone(LIMA_ZONE);
            
            DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(
                driverId,
                inicioNocturno.format(DATETIME_FORMATTER),
                finNocturno.format(DATETIME_FORMATTER),
                null
            );
            
            if (respuesta != null && respuesta.getOrders() != null && !respuesta.getOrders().isEmpty()) {
                // Filtrar viajes nocturnos (desde las 18:00 del día anterior hasta las 04:59 del día actual)
                List<OrderInfoResponse> viajesNocturnos = filtrarViajesNocturnos(respuesta.getOrders());
                
                if (!viajesNocturnos.isEmpty()) {
                    // Verificar si el último viaje nocturno termina cerca del viaje de madrugada
                    LocalDateTime ultimoFinViajeNocturno = obtenerUltimoFinViaje(viajesNocturnos);
                    if (ultimoFinViajeNocturno != null) {
                        long minutosGap = Duration.between(ultimoFinViajeNocturno, fechaViaje).toMinutes();
                        if (minutosGap >= 0 && minutosGap < INACTIVIDAD_MAXIMA_MINUTOS) {
                            log.info("🌙 [CalculatedShiftService] Viaje a las {} pertenece a turno nocturno del día anterior. Gap: {} minutos (máximo: {}). Último fin nocturno: {}", 
                                fechaViaje, minutosGap, INACTIVIDAD_MAXIMA_MINUTOS, ultimoFinViajeNocturno);
                            return true;
                        } else {
                            log.info("⏸️ [CalculatedShiftService] Viaje a las {} NO pertenece a turno nocturno del día anterior. Gap: {} minutos (máximo: {}). Último fin nocturno: {}", 
                                fechaViaje, minutosGap, INACTIVIDAD_MAXIMA_MINUTOS, ultimoFinViajeNocturno);
                        }
                    } else {
                        // Si hay viajes nocturnos pero no tienen fecha de fin, considerar que pertenece al turno nocturno
                        log.info("🌙 [CalculatedShiftService] Viaje a las {} pertenece a turno nocturno del día anterior. Viajes nocturnos encontrados: {} (sin fecha de fin)", 
                            fechaViaje, viajesNocturnos.size());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error verificando turno nocturno del día anterior para driver_id {}: {}", 
                driverId, e.getMessage());
        }
        
        return false;
    }
    
    private long contarViajesNocturnosDelDiaAnterior(List<OrderInfoResponse> viajes, LocalDate diaAnterior) {
        return viajes.stream()
            .filter(viaje -> viaje.getBookedAt() != null)
            .map(viaje -> {
                try {
                    ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
                    return bookedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .filter(fecha -> {
                LocalDate fechaViajeAnterior = fecha.toLocalDate();
                int hora = fecha.getHour();
                return fechaViajeAnterior.equals(diaAnterior) && hora >= HORA_INICIO_NOCTURNO;
            })
            .count();
    }
    
    // ==================== PRIVATE METHODS: TRIP FETCHING ====================
    
    private ViajesDelDia obtenerViajesDelDia(String driverId, LocalDate fecha) {
        ZonedDateTime inicioDia = fecha.atTime(HORA_MEDIANOCHE, 0, 0).atZone(LIMA_ZONE);
        ZonedDateTime finDiurno = fecha.atTime(HORA_FIN_DIURNO - 1, MINUTO_FIN_DIA, SEGUNDO_FIN_DIA).atZone(LIMA_ZONE);
        
        DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(
            driverId,
            inicioDia.format(API_DATE_FORMATTER),
            finDiurno.format(API_DATE_FORMATTER),
            null
        );
        
        List<OrderInfoResponse> todosViajes = respuesta.getOrders() != null ? respuesta.getOrders() : new ArrayList<>();
        
        return separarViajesPorTipo(todosViajes, fecha);
    }
    
    private ViajesDelDia separarViajesPorTipo(List<OrderInfoResponse> todosViajes, LocalDate fecha) {
        List<OrderInfoResponse> viajesMadrugada = new ArrayList<>();
        List<OrderInfoResponse> viajesDiurnos = new ArrayList<>();
        
        LocalDateTime limiteDiurno = fecha.atTime(HORA_FIN_DIURNO, 0, 0);
        
        for (OrderInfoResponse viaje : todosViajes) {
            if (viaje.getBookedAt() == null) {
                continue;
            }
            
            try {
                LocalDateTime inicioViaje = obtenerFechaInicioViaje(viaje);
                LocalDateTime finViaje = obtenerFechaFinViaje(viaje);
                
                if (inicioViaje == null) {
                    continue;
                }
                
                int horaInicio = inicioViaje.getHour();
                
                // Si el viaje termina después de las 18:00, debe ir al turno nocturno (no al diurno)
                // Esto asegura que los viajes que cruzan las 18:00 se clasifiquen correctamente
                if (finViaje != null && finViaje.isAfter(limiteDiurno)) {
                    // Este viaje cruza las 18:00, no debe estar en viajes diurnos
                    // Se procesará en el turno nocturno
                    continue;
                }
                
                if (horaInicio < HORA_INICIO_DIURNO) {
                    viajesMadrugada.add(viaje);
                } else if (horaInicio < HORA_FIN_DIURNO) {
                    viajesDiurnos.add(viaje);
                }
            } catch (Exception e) {
                log.debug("Error filtrando viaje del día: {}", e.getMessage());
            }
        }
        
        return new ViajesDelDia(viajesMadrugada, viajesDiurnos);
    }
    
    private int obtenerHoraViaje(OrderInfoResponse viaje) {
        try {
            ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
            LocalDateTime bookedDateTime = bookedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
            return bookedDateTime.getHour();
        } catch (Exception e) {
            return -1;
        }
    }
    
    private ViajesNocturnos obtenerViajesNocturnos(String driverId, LocalDate fecha, boolean consultarDiaAnterior) {
        LocalDate fechaAnterior = fecha.minusDays(1);
        LocalDate fechaSiguiente = fecha.plusDays(1);
        
        List<OrderInfoResponse> viajesNocturnosAnterior = consultarDiaAnterior
            ? obtenerViajesNocturnosRango(driverId, fechaAnterior, fecha, "día anterior")
            : new ArrayList<>();
        
        if (consultarDiaAnterior) {
            log.info("📊 [CalculatedShiftService] Se consultó el día anterior porque hay viajes de madrugada");
        } else {
            log.info("📊 [CalculatedShiftService] NO se consultó el día anterior porque el primer viaje del día es después de las 5 AM");
        }
        
        List<OrderInfoResponse> viajesNocturnosActual = obtenerViajesNocturnosRango(
            driverId, fecha, fechaSiguiente, "día actual");
        
        return new ViajesNocturnos(viajesNocturnosAnterior, viajesNocturnosActual, fechaAnterior, fechaSiguiente);
    }
    
    private List<OrderInfoResponse> obtenerViajesNocturnosRango(
            String driverId, LocalDate fechaInicio, LocalDate fechaFin, String tipo) {
        
        // Consultar desde 1 hora antes de las 18:00 (17:00) para capturar viajes tempranos
        // que puedan pertenecer al turno nocturno si están dentro del rango de inactividad
        ZonedDateTime inicioBusqueda = fechaInicio.atTime(HORA_INICIO_NOCTURNO - 1, 0, 0).atZone(LIMA_ZONE);
        ZonedDateTime finNocturno = fechaFin.atTime(HORA_FIN_NOCTURNO - 1, MINUTO_FIN_DIA, SEGUNDO_FIN_DIA).atZone(LIMA_ZONE);
        
        DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(
            driverId,
            inicioBusqueda.format(API_DATE_FORMATTER),
            finNocturno.format(API_DATE_FORMATTER),
            null
        );
        
        List<OrderInfoResponse> todosViajes = respuesta.getOrders() != null ? respuesta.getOrders() : new ArrayList<>();
        List<OrderInfoResponse> viajesNocturnos = filtrarViajesNocturnos(todosViajes);
        
        // Buscar viajes anteriores (antes de las 18:00) que pertenezcan al turno nocturno
        // si están dentro del rango de inactividad del primer viaje nocturno
        if (!viajesNocturnos.isEmpty()) {
            viajesNocturnos = incluirViajesAnterioresSiPertenecen(todosViajes, viajesNocturnos);
        }
        
        // Log detallado de los viajes encontrados
        if (!viajesNocturnos.isEmpty()) {
            LocalDateTime primerViaje = obtenerPrimerViaje(viajesNocturnos);
            LocalDateTime ultimoViaje = obtenerUltimoViaje(viajesNocturnos);
            log.info("📊 [CalculatedShiftService] Viajes nocturnos del {} encontrados: {} de {} totales. Rango: {} - {}", 
                tipo, viajesNocturnos.size(), todosViajes.size(), 
                primerViaje != null ? primerViaje.toLocalTime() : "N/A",
                ultimoViaje != null ? ultimoViaje.toLocalTime() : "N/A");
        } else {
            log.info("📊 [CalculatedShiftService] Viajes nocturnos del {} encontrados: {} de {} totales", 
                tipo, viajesNocturnos.size(), todosViajes.size());
        }
        
        return viajesNocturnos;
    }
    
    private List<OrderInfoResponse> incluirViajesAnterioresSiPertenecen(
            List<OrderInfoResponse> todosViajes, List<OrderInfoResponse> viajesNocturnos) {
        
        if (viajesNocturnos.isEmpty()) {
            return viajesNocturnos;
        }
        
        // Obtener el primer viaje nocturno (el más temprano)
        OrderInfoResponse primerViajeNocturno = viajesNocturnos.stream()
            .min(Comparator.comparing(v -> {
                LocalDateTime inicio = obtenerFechaInicioViaje(v);
                return inicio != null ? inicio : LocalDateTime.MAX;
            }))
            .orElse(null);
        
        if (primerViajeNocturno == null) {
            return viajesNocturnos;
        }
        
        LocalDateTime inicioPrimerViajeNocturno = obtenerFechaInicioViaje(primerViajeNocturno);
        if (inicioPrimerViajeNocturno == null) {
            return viajesNocturnos;
        }
        
        // Buscar viajes anteriores (antes de las 18:00) que estén dentro del rango de inactividad
        // Buscar hacia atrás desde el primer viaje nocturno
        List<OrderInfoResponse> viajesAnteriores = new ArrayList<>();
        LocalDateTime puntoReferencia = inicioPrimerViajeNocturno;
        
        // Ordenar todos los viajes por fecha de inicio para buscar hacia atrás
        List<OrderInfoResponse> todosViajesOrdenados = ordenarViajesPorFechaInicio(todosViajes);
        
        // Buscar viajes anteriores que terminen cerca del inicio del primer viaje nocturno
        for (int i = todosViajesOrdenados.size() - 1; i >= 0; i--) {
            OrderInfoResponse viaje = todosViajesOrdenados.get(i);
            
            // Si ya llegamos a un viaje nocturno, detener la búsqueda
            if (viajesNocturnos.contains(viaje)) {
                break;
            }
            
            LocalDateTime inicioViaje = obtenerFechaInicioViaje(viaje);
            LocalDateTime finViaje = obtenerFechaFinViaje(viaje);
            
            if (inicioViaje == null || finViaje == null) {
                continue;
            }
            
            // Solo considerar viajes antes de las 18:00 (17:00 - 17:59)
            int horaViaje = inicioViaje.getHour();
            if (horaViaje >= 17 && horaViaje < HORA_INICIO_NOCTURNO) {
                // Calcular el gap entre el fin de este viaje y el inicio del primer viaje nocturno
                long minutosGap = Duration.between(finViaje, puntoReferencia).toMinutes();
                
                // Si el gap es menor a 4 horas, este viaje pertenece al turno nocturno
                if (minutosGap >= 0 && minutosGap < INACTIVIDAD_MAXIMA_MINUTOS) {
                    viajesAnteriores.add(0, viaje); // Agregar al inicio para mantener orden cronológico
                    puntoReferencia = inicioViaje; // Actualizar punto de referencia para buscar más hacia atrás
                    log.info("🌙 [CalculatedShiftService] Viaje anterior a las {} (fin: {}) incluido en turno nocturno - gap: {} minutos con viaje a las {}", 
                        inicioViaje.toLocalTime(), finViaje.toLocalTime(), minutosGap, inicioPrimerViajeNocturno.toLocalTime());
                } else {
                    // Si el gap es mayor a 4 horas, detener la búsqueda hacia atrás
                    break;
                }
            }
        }
        
        // Agregar viajes anteriores al inicio de la lista (orden cronológico)
        if (!viajesAnteriores.isEmpty()) {
            List<OrderInfoResponse> viajesCompletos = new ArrayList<>(viajesAnteriores);
            viajesCompletos.addAll(viajesNocturnos);
            // Ordenar todos los viajes por fecha de inicio
            return ordenarViajesPorFechaInicio(viajesCompletos);
        }
        
        return viajesNocturnos;
    }
    
    private List<OrderInfoResponse> filtrarViajesNocturnos(List<OrderInfoResponse> todosViajes) {
        List<OrderInfoResponse> viajesNocturnos = new ArrayList<>();
        
        for (OrderInfoResponse viaje : todosViajes) {
            if (viaje.getBookedAt() == null) {
                continue;
            }
            
            try {
                LocalDateTime inicioViaje = obtenerFechaInicioViaje(viaje);
                LocalDateTime finViaje = obtenerFechaFinViaje(viaje);
                
                if (inicioViaje == null) {
                    continue;
                }
                
                int horaInicio = inicioViaje.getHour();
                
                // Un viaje es nocturno si:
                // 1. Empieza a las 18:00 o después (hora >= 18)
                // 2. O empieza antes de las 5:00 AM (hora < 5)
                // 3. O cruza las 18:00 (termina después de las 18:00 aunque empiece antes)
                boolean esNocturno = esViajeNocturno(horaInicio);
                
                // Si el viaje cruza las 18:00 (empieza antes pero termina después), es nocturno
                if (!esNocturno && finViaje != null) {
                    LocalDate fechaViaje = inicioViaje.toLocalDate();
                    LocalDateTime limiteDiurno = fechaViaje.atTime(HORA_FIN_DIURNO, 0, 0);
                    if (finViaje.isAfter(limiteDiurno)) {
                        esNocturno = true;
                        log.debug("🌙 [CalculatedShiftService] Viaje cruza las 18:00 - agregado como nocturno: {} - inicio: {}, fin: {}", 
                            viaje.getId(), inicioViaje.toLocalTime(), finViaje.toLocalTime());
                    }
                }
                
                if (esNocturno) {
                    viajesNocturnos.add(viaje);
                    log.debug("🌙 [CalculatedShiftService] Viaje nocturno agregado: {} - hora inicio: {}", 
                        viaje.getId(), horaInicio);
                }
            } catch (Exception e) {
                log.debug("Error filtrando viaje nocturno: {}", e.getMessage());
            }
        }
        
        return viajesNocturnos;
    }
    
    private boolean esViajeNocturno(int hora) {
        return hora >= HORA_INICIO_NOCTURNO || hora < HORA_FIN_NOCTURNO;
    }
    
    // ==================== PRIVATE METHODS: PROCESSING ====================
    
    private void procesarViajesMadrugada(ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos, 
                                         String driverId, LocalDate fecha) {
        if (viajesDelDia.viajesMadrugada.isEmpty()) {
            return;
        }
        
        Set<String> idsViajesNocturnosAnterior = obtenerIdsViajes(viajesNocturnos.viajesNocturnosAnterior);
        Set<String> idsViajesMadrugada = obtenerIdsViajes(viajesDelDia.viajesMadrugada);
        
        if (idsViajesNocturnosAnterior.containsAll(idsViajesMadrugada)) {
            limpiarViajesMadrugadaYaProcesados(viajesDelDia);
            return;
        }
        
        LocalDateTime primerViajeMadrugada = obtenerPrimerViaje(viajesDelDia.viajesMadrugada);
        boolean perteneceAlDiaAnterior = verificarSiPerteneceAlDiaAnterior(
            viajesNocturnos, primerViajeMadrugada, driverId);
        
        if (perteneceAlDiaAnterior) {
            moverViajesMadrugadaATurnoNocturno(viajesDelDia, viajesNocturnos, primerViajeMadrugada);
        } else {
            moverViajesMadrugadaATurnoNocturnoActual(viajesDelDia, viajesNocturnos, idsViajesNocturnosAnterior, primerViajeMadrugada);
        }
    }
    
    private Set<String> obtenerIdsViajes(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .map(OrderInfoResponse::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    private void limpiarViajesMadrugadaYaProcesados(ViajesDelDia viajesDelDia) {
        log.info("🌙 [CalculatedShiftService] Todos los viajes de madrugada ({} viajes) ya están en el turno nocturno del día anterior. Limpiando lista.", 
            viajesDelDia.viajesMadrugada.size());
        viajesDelDia.viajesMadrugada.clear();
    }
    
    private boolean verificarSiPerteneceAlDiaAnterior(ViajesNocturnos viajesNocturnos, 
                                                       LocalDateTime primerViajeMadrugada, 
                                                       String driverId) {
        if (viajesNocturnos.viajesNocturnosAnterior.isEmpty() || primerViajeMadrugada == null) {
            return false;
        }
        
        LocalDateTime ultimoFinViajeNocturno = obtenerUltimoFinViaje(viajesNocturnos.viajesNocturnosAnterior);
        if (ultimoFinViajeNocturno != null) {
            long minutosGap = Duration.between(ultimoFinViajeNocturno, primerViajeMadrugada).toMinutes();
            if (minutosGap >= 0 && minutosGap < INACTIVIDAD_MAXIMA_MINUTOS) {
                log.info("🌙 [CalculatedShiftService] Viaje de madrugada a las {} está dentro del rango de inactividad ({} minutos) del turno nocturno del día anterior (último fin: {}).", 
                    primerViajeMadrugada, minutosGap, ultimoFinViajeNocturno);
                return true;
            } else {
                log.info("⏸️ [CalculatedShiftService] Viaje de madrugada a las {} NO está dentro del rango de inactividad (gap: {} minutos, máximo: {}). Último fin nocturno: {}", 
                    primerViajeMadrugada, minutosGap, INACTIVIDAD_MAXIMA_MINUTOS, ultimoFinViajeNocturno);
            }
        }
        
        return esTurnoNocturnoDelDiaAnterior(driverId, primerViajeMadrugada);
    }
    
    private void moverViajesMadrugadaATurnoNocturnoActual(ViajesDelDia viajesDelDia, 
                                                           ViajesNocturnos viajesNocturnos,
                                                           Set<String> idsViajesNocturnosAnterior,
                                                           LocalDateTime primerViajeMadrugada) {
        List<OrderInfoResponse> viajesMadrugadaNoEnNocturno = viajesDelDia.viajesMadrugada.stream()
            .filter(viaje -> viaje.getId() == null || !idsViajesNocturnosAnterior.contains(viaje.getId()))
            .collect(Collectors.toList());
        
        if (!viajesMadrugadaNoEnNocturno.isEmpty()) {
            log.info("🌙 [CalculatedShiftService] Viajes de madrugada ({} viajes, primer viaje: {}) no pertenecen al turno nocturno del día anterior. Agregando al turno nocturno del día actual.", 
                viajesMadrugadaNoEnNocturno.size(), primerViajeMadrugada);
            viajesNocturnos.viajesNocturnosActual.addAll(viajesMadrugadaNoEnNocturno);
            viajesDelDia.viajesMadrugada.clear();
        } else {
            log.info("🌙 [CalculatedShiftService] Todos los viajes de madrugada ya están en el turno nocturno. No se crearán turnos diurnos duplicados.");
            viajesDelDia.viajesMadrugada.clear();
        }
    }
    
    private void moverViajesMadrugadaATurnoNocturno(ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos, 
                                                     LocalDateTime primerViaje) {
        log.info("🌙 [CalculatedShiftService] Viajes de madrugada ({} viajes, primer viaje: {}) pertenecen a turno nocturno del día anterior. Moviendo a turno nocturno.", 
            viajesDelDia.viajesMadrugada.size(), primerViaje);
        
        // Eliminar duplicados: los viajes de madrugada pueden estar ya en viajesNocturnosAnterior
        // porque la consulta del día anterior incluye viajes hasta las 04:59
        Set<String> idsViajesNocturnos = viajesNocturnos.viajesNocturnosAnterior.stream()
            .map(OrderInfoResponse::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        // Solo agregar viajes de madrugada que no estén ya en el turno nocturno
        List<OrderInfoResponse> viajesMadrugadaSinDuplicados = viajesDelDia.viajesMadrugada.stream()
            .filter(viaje -> viaje.getId() != null && !idsViajesNocturnos.contains(viaje.getId()))
            .collect(Collectors.toList());
        
        if (!viajesMadrugadaSinDuplicados.isEmpty()) {
            viajesNocturnos.viajesNocturnosAnterior.addAll(viajesMadrugadaSinDuplicados);
            log.info("✅ [CalculatedShiftService] Se agregaron {} viajes de madrugada al turno nocturno ({} ya estaban incluidos)", 
                viajesMadrugadaSinDuplicados.size(), 
                viajesDelDia.viajesMadrugada.size() - viajesMadrugadaSinDuplicados.size());
            
            // Log del rango completo del turno nocturno después de agregar viajes de madrugada
            if (!viajesNocturnos.viajesNocturnosAnterior.isEmpty()) {
                LocalDateTime primerViajeTurno = obtenerPrimerViaje(viajesNocturnos.viajesNocturnosAnterior);
                LocalDateTime ultimoViajeTurno = obtenerUltimoViaje(viajesNocturnos.viajesNocturnosAnterior);
                log.info("🌙 [CalculatedShiftService] Turno nocturno del día anterior ahora tiene {} viajes. Rango: {} - {}", 
                    viajesNocturnos.viajesNocturnosAnterior.size(),
                    primerViajeTurno != null ? primerViajeTurno.toLocalTime() : "N/A",
                    ultimoViajeTurno != null ? ultimoViajeTurno.toLocalTime() : "N/A");
            }
        } else {
            log.info("ℹ️ [CalculatedShiftService] Todos los viajes de madrugada ya estaban en el turno nocturno del día anterior");
        }
        
        viajesDelDia.viajesMadrugada.clear();
    }
    
    private void moverViajesMadrugadaATurnoDiurno(ViajesDelDia viajesDelDia, LocalDateTime primerViaje) {
        log.info("📅 [CalculatedShiftService] Viajes de madrugada ({} viajes, primer viaje: {}) NO pertenecen a turno nocturno anterior. Incluyendo en turno diurno.", 
            viajesDelDia.viajesMadrugada.size(), primerViaje);
        viajesDelDia.viajesDiurnos.addAll(viajesDelDia.viajesMadrugada);
        viajesDelDia.viajesMadrugada.clear();
    }
    
    private void guardarTurnos(String driverId, LocalDate fecha, ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos) {
        // Verificar si el turno nocturno del día anterior y el turno diurno del día actual deben unificarse
        boolean debenUnificarseNocturnoAnteriorYDiurno = verificarSiDebenUnificarse(
            viajesNocturnos.viajesNocturnosAnterior, 
            viajesDelDia.viajesDiurnos,
            viajesNocturnos.fechaAnterior,
            fecha
        );
        
        // Verificar si el turno diurno del día actual y el turno nocturno del día actual deben unificarse
        boolean debenUnificarseDiurnoYNocturnoActual = verificarSiDebenUnificarseDiurnoYNocturnoActual(
            viajesDelDia.viajesDiurnos,
            viajesNocturnos.viajesNocturnosActual
        );
        
        boolean seGuardoTurnoNocturnoAnterior = false;
        
        if (debenUnificarseNocturnoAnteriorYDiurno && !viajesNocturnos.viajesNocturnosAnterior.isEmpty() && !viajesDelDia.viajesDiurnos.isEmpty()) {
            unificarYGuardarTurnos(driverId, fecha, viajesDelDia, viajesNocturnos);
            seGuardoTurnoNocturnoAnterior = true;
        } else if (debenUnificarseDiurnoYNocturnoActual && !viajesDelDia.viajesDiurnos.isEmpty() && !viajesNocturnos.viajesNocturnosActual.isEmpty()) {
            unificarYGuardarTurnosDiurnoYNocturnoActual(driverId, fecha, viajesDelDia, viajesNocturnos);
            seGuardoTurnoNocturnoAnterior = false; // No se guardó el del día anterior
        } else {
            guardarTurnosPorSeparado(driverId, fecha, viajesDelDia, viajesNocturnos);
            // Si se guardó el turno nocturno del día anterior, marcar que ya se guardó
            seGuardoTurnoNocturnoAnterior = !viajesNocturnos.viajesNocturnosAnterior.isEmpty();
        }
        
        // Solo guardar turno nocturno del día actual si NO se guardó el del día anterior y NO se unificó con el diurno
        // Porque si ya se guardó el del día anterior (con fecha del día actual), no necesitamos el del día actual
        if (!seGuardoTurnoNocturnoAnterior && !debenUnificarseDiurnoYNocturnoActual) {
            guardarTurnoNocturnoActual(driverId, fecha, viajesNocturnos);
        } else {
            if (seGuardoTurnoNocturnoAnterior) {
                log.info("⏭️ [CalculatedShiftService] Turno nocturno del día anterior ya guardado con fecha {}. Omitiendo turno nocturno del día actual para evitar duplicados.", fecha);
            }
            if (debenUnificarseDiurnoYNocturnoActual) {
                log.info("⏭️ [CalculatedShiftService] Turno nocturno del día actual ya unificado con turno diurno. Omitiendo guardado separado.");
            }
        }
        
        validarViajesEncontrados(driverId, fecha, viajesDelDia, viajesNocturnos);
    }
    
    private void unificarYGuardarTurnos(String driverId, LocalDate fecha, ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos) {
        log.info("🔗 [CalculatedShiftService] Unificando turno nocturno del {} y turno diurno del {} - gap menor a 4 horas", 
            viajesNocturnos.fechaAnterior, fecha);
        
        List<OrderInfoResponse> viajesUnificados = new ArrayList<>();
        viajesUnificados.addAll(viajesNocturnos.viajesNocturnosAnterior);
        viajesUnificados.addAll(viajesDelDia.viajesDiurnos);
        
        CalculatedShift.TipoTurno tipoTurnoUnificado = determinarTipoTurnoUnificado(viajesUnificados);
        // Guardar con fecha del día actual (fecha), no del día anterior
        // El turno nocturno del día anterior pertenece al día que se está calculando
        procesarTurno(driverId, fecha, viajesUnificados, tipoTurnoUnificado);
        
        viajesNocturnos.viajesNocturnosAnterior.clear();
        viajesDelDia.viajesDiurnos.clear();
    }
    
    private CalculatedShift.TipoTurno determinarTipoTurnoUnificado(List<OrderInfoResponse> viajesUnificados) {
        LocalDateTime primerViaje = obtenerPrimerViaje(viajesUnificados);
        return (primerViaje != null && primerViaje.getHour() >= HORA_INICIO_NOCTURNO)
            ? CalculatedShift.TipoTurno.nocturno
            : CalculatedShift.TipoTurno.diurno;
    }
    
    private void guardarTurnosPorSeparado(String driverId, LocalDate fecha, ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos) {
        if (!viajesDelDia.viajesDiurnos.isEmpty()) {
            procesarTurno(driverId, fecha, viajesDelDia.viajesDiurnos, CalculatedShift.TipoTurno.diurno);
        }
        
        if (!viajesNocturnos.viajesNocturnosAnterior.isEmpty()) {
            // El turno nocturno del día anterior siempre se guarda con la fecha del día anterior
            // porque si no se unificó con el turno diurno del día actual, significa que son turnos separados
            LocalDate fechaParaGuardar = viajesNocturnos.fechaAnterior;
            
            LocalDateTime finTurnoNocturno = obtenerUltimoFinViaje(viajesNocturnos.viajesNocturnosAnterior);
            if (finTurnoNocturno != null) {
                log.info("🌙 [CalculatedShiftService] Turno nocturno del día anterior (termina en {}) NO se unificó con turno diurno del día actual. Guardando con fecha del día anterior: {}.", 
                    finTurnoNocturno, fechaParaGuardar);
            } else {
                log.info("🌙 [CalculatedShiftService] Guardando turno nocturno del día anterior con fecha del día anterior: {}.", 
                    fechaParaGuardar);
            }
            
            procesarTurno(driverId, fechaParaGuardar, viajesNocturnos.viajesNocturnosAnterior, 
                CalculatedShift.TipoTurno.nocturno);
        }
    }
    
    private void guardarTurnoNocturnoActual(String driverId, LocalDate fecha, ViajesNocturnos viajesNocturnos) {
        if (!viajesNocturnos.viajesNocturnosActual.isEmpty()) {
            log.info("🌙 [CalculatedShiftService] Guardando turno nocturno del día actual ({} viajes) para driver_id: {}, fecha: {}", 
                viajesNocturnos.viajesNocturnosActual.size(), driverId, fecha);
            procesarTurno(driverId, fecha, viajesNocturnos.viajesNocturnosActual, CalculatedShift.TipoTurno.nocturno);
        } else {
            log.info("⏭️ [CalculatedShiftService] No hay viajes nocturnos del día actual para guardar - driver_id: {}, fecha: {}", 
                driverId, fecha);
        }
    }
    
    private void validarViajesEncontrados(String driverId, LocalDate fecha, ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos) {
        if (viajesDelDia.viajesDiurnos.isEmpty() && viajesNocturnos.viajesNocturnosAnterior.isEmpty() 
            && viajesNocturnos.viajesNocturnosActual.isEmpty()) {
            log.warn("⚠️ [CalculatedShiftService] No se encontraron viajes para driver_id: {}, fecha: {}", driverId, fecha);
        }
    }
    
    private boolean verificarSiDebenUnificarse(List<OrderInfoResponse> viajesNocturnosAnterior, 
                                               List<OrderInfoResponse> viajesDiurnos,
                                               LocalDate fechaAnterior, LocalDate fechaActual) {
        if (viajesNocturnosAnterior.isEmpty() || viajesDiurnos.isEmpty()) {
            return false;
        }
        
        try {
            // Obtener el fin del último viaje del turno nocturno del día anterior
            LocalDateTime finTurnoNocturno = obtenerUltimoFinViaje(viajesNocturnosAnterior);
            
            // Obtener el primer viaje del turno diurno del día actual
            LocalDateTime inicioTurnoDiurno = obtenerPrimerViaje(viajesDiurnos);
            
            if (finTurnoNocturno != null && inicioTurnoDiurno != null) {
                // Calcular el gap entre el fin del turno nocturno y el inicio del turno diurno
                long minutosGap = Duration.between(finTurnoNocturno, inicioTurnoDiurno).toMinutes();
                
                // Si el gap es menor a 5 horas (300 minutos), deben unificarse
                if (minutosGap >= 0 && minutosGap < INACTIVIDAD_MAXIMA_MINUTOS) {
                    log.info("🔗 [CalculatedShiftService] Gap detectado: {} minutos ({} horas) entre turno nocturno ({}) y diurno ({}) - UNIFICANDO", 
                        minutosGap, minutosGap / 60.0, finTurnoNocturno, inicioTurnoDiurno);
                    return true;
                } else {
                    log.info("⏸️ [CalculatedShiftService] Gap entre turno nocturno ({}) y diurno ({}) es {} minutos (máximo: {}). NO se unificarán.", 
                        finTurnoNocturno, inicioTurnoDiurno, minutosGap, INACTIVIDAD_MAXIMA_MINUTOS);
                }
            }
        } catch (Exception e) {
            log.debug("Error verificando si deben unificarse: {}", e.getMessage());
        }
        
        return false;
    }
    
    private boolean verificarSiDebenUnificarseDiurnoYNocturnoActual(List<OrderInfoResponse> viajesDiurnos,
                                                                     List<OrderInfoResponse> viajesNocturnosActual) {
        // Los turnos diurnos y nocturnos del mismo día NUNCA se unifican
        // Deben estar separados en las 18:00 (HORA_FIN_DIURNO)
        // El turno diurno termina a las 18:00 y el turno nocturno empieza a las 18:00
        return false;
    }
    
    private void unificarYGuardarTurnosDiurnoYNocturnoActual(String driverId, LocalDate fecha, 
                                                             ViajesDelDia viajesDelDia, 
                                                             ViajesNocturnos viajesNocturnos) {
        log.info("🔗 [CalculatedShiftService] Unificando turno diurno y turno nocturno del día {} - gap menor a 5 horas", fecha);
        
        List<OrderInfoResponse> viajesUnificados = new ArrayList<>();
        viajesUnificados.addAll(viajesDelDia.viajesDiurnos);
        viajesUnificados.addAll(viajesNocturnos.viajesNocturnosActual);
        
        // El tipo de turno unificado será diurno porque empieza antes de las 18:00
        CalculatedShift.TipoTurno tipoTurnoUnificado = determinarTipoTurnoUnificado(viajesUnificados);
        
        procesarTurno(driverId, fecha, viajesUnificados, tipoTurnoUnificado);
        
        // Limpiar las listas ya que se unificaron
        viajesDelDia.viajesDiurnos.clear();
        viajesNocturnos.viajesNocturnosActual.clear();
    }
    
    private void procesarTurno(String driverId, LocalDate fecha, List<OrderInfoResponse> viajes, 
                              CalculatedShift.TipoTurno tipoTurno) {
        if (viajes.isEmpty()) {
            return;
        }
        
        HorasTurno horasTurno = calcularHorasTurno(viajes, tipoTurno, fecha);
        if (!validarHorasTurno(horasTurno, tipoTurno, driverId, fecha)) {
            return;
        }
        
        tipoTurno = corregirTipoTurnoSiEsNecesario(horasTurno.horaInicio, tipoTurno);
        viajes = procesarExtensionesTurno(driverId, fecha, viajes, horasTurno.horaInicio, tipoTurno);
        horasTurno = recalcularHorasTurno(viajes, tipoTurno, fecha);
        
        if (horasTurno.horaInicio == null) {
            return;
        }
        
        guardarTurnoCalculado(driverId, fecha, tipoTurno, horasTurno, viajes);
    }
    
    private boolean validarHorasTurno(HorasTurno horasTurno, CalculatedShift.TipoTurno tipoTurno, 
                                      String driverId, LocalDate fecha) {
        if (horasTurno.horaInicio == null) {
            log.warn("⚠️ [CalculatedShiftService] No se pudo determinar hora de inicio para turno {} - driver_id: {}, fecha: {}", 
                tipoTurno, driverId, fecha);
            return false;
        }
        return true;
    }
    
    private CalculatedShift.TipoTurno corregirTipoTurnoSiEsNecesario(LocalDateTime horaInicio, 
                                                                     CalculatedShift.TipoTurno tipoTurno) {
        if (horaInicio.getHour() < HORA_INICIO_DIURNO && tipoTurno == CalculatedShift.TipoTurno.diurno) {
            log.info("🌙 [CalculatedShiftService] Corrigiendo tipo de turno: hora de inicio {} es antes de las 5 AM, cambiando de diurno a nocturno", 
                horaInicio.toLocalTime());
            return CalculatedShift.TipoTurno.nocturno;
        }
        return tipoTurno;
    }
    
    private List<OrderInfoResponse> procesarExtensionesTurno(String driverId, LocalDate fecha, 
                                                             List<OrderInfoResponse> viajes, 
                                                             LocalDateTime horaInicio, 
                                                             CalculatedShift.TipoTurno tipoTurno) {
        if (horaInicio.getHour() < HORA_INICIO_DIURNO) {
            viajes = buscarYAgregarViajesNocturnosDelDiaAnterior(driverId, fecha, viajes, horaInicio);
        }
        
        if (tipoTurno == CalculatedShift.TipoTurno.nocturno) {
            viajes = extenderTurnoNocturnoSiEsNecesario(driverId, fecha, viajes);
        }
        
        return viajes;
    }
    
    private HorasTurno recalcularHorasTurno(List<OrderInfoResponse> viajes, CalculatedShift.TipoTurno tipoTurno, LocalDate fecha) {
        HorasTurno horasTurno = calcularHorasTurno(viajes, tipoTurno, fecha);
        if (horasTurno.horaInicio == null) {
            log.warn("⚠️ [CalculatedShiftService] No se pudo determinar hora de inicio después de procesar extensiones - fecha: {}", 
                fecha);
        }
        return horasTurno;
    }
    
    private void guardarTurnoCalculado(String driverId, LocalDate fecha, CalculatedShift.TipoTurno tipoTurno, 
                                      HorasTurno horasTurno, List<OrderInfoResponse> viajes) {
        CalculatedShift turno = obtenerOCrearTurno(driverId, fecha, tipoTurno);
        Double montoTotal = calcularMontoTotal(viajes);
        actualizarTurno(turno, tipoTurno, horasTurno, fecha, montoTotal);
        guardarTurno(turno, driverId, fecha, tipoTurno, horasTurno, montoTotal);
    }
    
    /**
     * Calcula el monto total que ganó el conductor en el turno
     * Suma: cash + card + price_tip + price_bonus + price_promotion + price_other
     * NOTA: NO se incluye 'price' porque es el precio base de Yango Pro, no el monto que recibe el conductor
     */
    private Double calcularMontoTotal(List<OrderInfoResponse> viajes) {
        if (viajes == null || viajes.isEmpty()) {
            return 0.0;
        }
        
        double montoTotal = viajes.stream()
            .mapToDouble(viaje -> {
                double total = 0.0;
                if (viaje.getCash() != null) {
                    total += viaje.getCash();
                }
                if (viaje.getCard() != null) {
                    total += viaje.getCard();
                }
                // NO se suma price porque es el precio base de Yango Pro, no el monto que recibe el conductor
                if (viaje.getPriceTip() != null) {
                    total += viaje.getPriceTip();
                }
                if (viaje.getPriceBonus() != null) {
                    total += viaje.getPriceBonus();
                }
                if (viaje.getPricePromotion() != null) {
                    total += viaje.getPricePromotion();
                }
                if (viaje.getPriceOther() != null) {
                    total += viaje.getPriceOther();
                }
                return total;
            })
            .sum();
        
        // Redondear a 2 decimales
        double montoTotalRedondeado = Math.round(montoTotal * 100.0) / 100.0;
        
        log.debug("💰 [CalculatedShiftService] Monto total calculado: {} soles para {} viajes (sin incluir price base)", montoTotalRedondeado, viajes.size());
        return montoTotalRedondeado;
    }
    
    /**
     * Busca y agrega viajes nocturnos del día anterior si el turno actual empieza antes de las 5 AM
     * y hay viajes nocturnos del día anterior dentro del rango de inactividad (5 horas)
     */
    private List<OrderInfoResponse> buscarYAgregarViajesNocturnosDelDiaAnterior(String driverId, LocalDate fecha, 
                                                                                List<OrderInfoResponse> viajesActuales, 
                                                                                LocalDateTime horaInicioTurnoActual) {
        if (viajesActuales.isEmpty() || horaInicioTurnoActual == null) {
            return viajesActuales;
        }
        
        LocalDate diaAnterior = fecha.minusDays(1);
        
        try {
            // Consultar desde las 17:00 del día anterior hasta las 04:59 del día actual
            ZonedDateTime inicioNocturno = diaAnterior.atTime(HORA_INICIO_NOCTURNO - 1, 0, 0).atZone(LIMA_ZONE);
            ZonedDateTime finNocturno = fecha.atTime(HORA_FIN_NOCTURNO - 1, MINUTO_FIN_DIA, SEGUNDO_FIN_DIA).atZone(LIMA_ZONE);
            
            DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(
                driverId,
                inicioNocturno.format(API_DATE_FORMATTER),
                finNocturno.format(API_DATE_FORMATTER),
                null
            );
            
            if (respuesta == null || respuesta.getOrders() == null || respuesta.getOrders().isEmpty()) {
                log.info("📊 [CalculatedShiftService] No se encontraron viajes del día anterior para turno que empieza a las {}", 
                    horaInicioTurnoActual.toLocalTime());
                return viajesActuales;
            }
            
            // Filtrar viajes nocturnos
            List<OrderInfoResponse> viajesNocturnosAnterior = filtrarViajesNocturnos(respuesta.getOrders());
            
            if (viajesNocturnosAnterior.isEmpty()) {
                log.info("📊 [CalculatedShiftService] No se encontraron viajes nocturnos del día anterior para turno que empieza a las {}", 
                    horaInicioTurnoActual.toLocalTime());
                return viajesActuales;
            }
            
            // Obtener el fin del último viaje nocturno del día anterior
            LocalDateTime ultimoFinViajeNocturno = obtenerUltimoFinViaje(viajesNocturnosAnterior);
            if (ultimoFinViajeNocturno == null) {
                return viajesActuales;
            }
            
            // Verificar si el último viaje nocturno del día anterior está dentro del rango de inactividad (5 horas)
            long minutosGap = Duration.between(ultimoFinViajeNocturno, horaInicioTurnoActual).toMinutes();
            
            if (minutosGap >= 0 && minutosGap < INACTIVIDAD_MAXIMA_MINUTOS) {
                log.info("🌙 [CalculatedShiftService] Turno que empieza a las {} está dentro del rango de inactividad ({} minutos) del turno nocturno del día anterior (último fin: {}). Agregando {} viajes del día anterior.", 
                    horaInicioTurnoActual.toLocalTime(), minutosGap, ultimoFinViajeNocturno.toLocalTime(), viajesNocturnosAnterior.size());
                
                // Agregar viajes nocturnos del día anterior al inicio de la lista (orden cronológico)
                List<OrderInfoResponse> viajesCompletos = new ArrayList<>(viajesNocturnosAnterior);
                viajesCompletos.addAll(viajesActuales);
                return ordenarViajesPorFechaInicio(viajesCompletos);
            } else {
                log.info("⏸️ [CalculatedShiftService] Turno que empieza a las {} NO está dentro del rango de inactividad (gap: {} minutos, máximo: {}) del turno nocturno del día anterior (último fin: {}).", 
                    horaInicioTurnoActual.toLocalTime(), minutosGap, INACTIVIDAD_MAXIMA_MINUTOS, ultimoFinViajeNocturno.toLocalTime());
            }
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error buscando viajes nocturnos del día anterior para driver_id {}: {}", 
                driverId, e.getMessage());
        }
        
        return viajesActuales;
    }
    
    private List<OrderInfoResponse> extenderTurnoNocturnoSiEsNecesario(String driverId, LocalDate fecha, 
                                                                        List<OrderInfoResponse> viajesNocturnos) {
        if (viajesNocturnos.isEmpty()) {
            return viajesNocturnos;
        }
        
        LocalDateTime ultimoViajeNocturno = obtenerUltimoViaje(viajesNocturnos);
        if (ultimoViajeNocturno == null) {
            return viajesNocturnos;
        }
        
        int horaUltimoViaje = ultimoViajeNocturno.getHour();
        int minutoUltimoViaje = ultimoViajeNocturno.getMinute();
        
        // Si el último viaje termina entre las 4:00 AM y las 7:59 AM, buscar viajes adicionales
        // Esto cubre turnos que terminan cerca del amanecer y pueden extenderse
        boolean debeExtender = (horaUltimoViaje == 4) || 
                               (horaUltimoViaje == 5) || 
                               (horaUltimoViaje == 6) ||
                               (horaUltimoViaje == 7);
        
        if (!debeExtender) {
            return viajesNocturnos;
        }
        
        log.info("🌙 [CalculatedShiftService] Turno nocturno termina a las {} - buscando viajes adicionales después de las 5 AM para extender el turno", 
            ultimoViajeNocturno.toLocalTime());
        
        // Buscar viajes adicionales desde las 5 AM hasta las 12 PM (mediodía) para asegurar que encontramos todos los viajes continuos
        LocalDate fechaSiguiente = fecha.plusDays(1);
        ZonedDateTime inicioBusqueda = fecha.atTime(HORA_INICIO_DIURNO, 0, 0).atZone(LIMA_ZONE);
        ZonedDateTime finBusqueda = fechaSiguiente.atTime(12, 0, 0).atZone(LIMA_ZONE); // Buscar hasta las 12 PM
        
        DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(
            driverId,
            inicioBusqueda.format(API_DATE_FORMATTER),
            finBusqueda.format(API_DATE_FORMATTER),
            null
        );
        
        List<OrderInfoResponse> viajesAdicionales = respuesta.getOrders() != null ? respuesta.getOrders() : new ArrayList<>();
        
        if (viajesAdicionales.isEmpty()) {
            log.info("📊 [CalculatedShiftService] No se encontraron viajes adicionales después de las 5 AM");
            return viajesNocturnos;
        }
        
        // Ordenar los viajes adicionales por fecha de inicio
        List<OrderInfoResponse> viajesAdicionalesOrdenados = ordenarViajesPorFechaInicio(viajesAdicionales);
        
        // Mantener todos los viajes nocturnos originales y agregar solo los adicionales que pertenecen al turno continuo
        List<OrderInfoResponse> viajesExtendidos = new ArrayList<>(viajesNocturnos);
        
        // Obtener la hora de fin del último viaje nocturno (no solo la hora de inicio)
        LocalDateTime ultimoFinViaje = obtenerUltimoFinViaje(viajesNocturnos);
        if (ultimoFinViaje == null) {
            ultimoFinViaje = ultimoViajeNocturno; // Fallback a la hora de inicio si no hay fin
        }
        
        log.info("🌙 [CalculatedShiftService] Último viaje nocturno termina a las {} - buscando viajes continuos", 
            ultimoFinViaje.toLocalTime());
        
        // Agregar viajes adicionales que no tengan un gap de 4 horas con el último viaje nocturno
        for (OrderInfoResponse viajeAdicional : viajesAdicionalesOrdenados) {
            LocalDateTime inicioViaje = obtenerFechaInicioViaje(viajeAdicional);
            LocalDateTime finViaje = obtenerFechaFinViaje(viajeAdicional);
            
            if (inicioViaje == null) {
                continue;
            }
            
            // Calcular el gap entre el fin del último viaje y el inicio de este viaje adicional
            long minutosGap = Duration.between(ultimoFinViaje, inicioViaje).toMinutes();
            
            if (minutosGap >= 0 && minutosGap < INACTIVIDAD_MAXIMA_MINUTOS) {
                // No hay gap significativo, agregar el viaje al turno nocturno
                viajesExtendidos.add(viajeAdicional);
                ultimoFinViaje = finViaje != null ? finViaje : inicioViaje;
                log.info("🌙 [CalculatedShiftService] Viaje adicional agregado al turno nocturno: inicio {} - fin {} - gap: {} minutos", 
                    inicioViaje.toLocalTime(), finViaje != null ? finViaje.toLocalTime() : "N/A", minutosGap);
            } else if (minutosGap < 0) {
                // El viaje adicional empieza antes del último viaje, puede ser un solapamiento - verificar si es continuo
                // Si el fin del viaje adicional es después del inicio del último viaje, puede ser parte del turno
                if (finViaje != null && finViaje.isAfter(ultimoFinViaje)) {
                    viajesExtendidos.add(viajeAdicional);
                    ultimoFinViaje = finViaje;
                    log.info("🌙 [CalculatedShiftService] Viaje adicional solapado agregado al turno nocturno: fin {} - extendiendo hasta {}", 
                        finViaje.toLocalTime(), ultimoFinViaje.toLocalTime());
                }
            } else {
                // Hay un gap de 4 horas o más, detener la extensión
                log.info("⏸️ [CalculatedShiftService] Gap de {} minutos ({} horas) detectado - deteniendo extensión del turno nocturno en {}", 
                    minutosGap, minutosGap / 60.0, ultimoFinViaje);
                break;
            }
        }
        
        if (viajesExtendidos.size() > viajesNocturnos.size()) {
            int viajesAgregados = viajesExtendidos.size() - viajesNocturnos.size();
            LocalDateTime nuevoFin = obtenerUltimoViaje(viajesExtendidos);
            log.info("✅ [CalculatedShiftService] Turno nocturno extendido: {} viajes adicionales agregados. Nuevo fin: {}", 
                viajesAgregados, nuevoFin);
        }
        
        return viajesExtendidos;
    }
    
    private HorasTurno calcularHorasTurno(List<OrderInfoResponse> viajes, CalculatedShift.TipoTurno tipoTurno, LocalDate fecha) {
        if (viajes == null || viajes.isEmpty()) {
            return crearHorasTurnoVacio();
        }
        
        // Filtrar viajes según el tipo de turno
        List<OrderInfoResponse> viajesFiltrados = filtrarViajesPorTipoTurno(viajes, tipoTurno, fecha);
        
        List<OrderInfoResponse> viajesOrdenados = ordenarViajesPorFechaInicio(viajesFiltrados);
        if (viajesOrdenados.isEmpty()) {
            return crearHorasTurnoVacio();
        }
        
        return procesarViajesParaCalcularHoras(viajesOrdenados, tipoTurno, fecha);
    }
    
    private List<OrderInfoResponse> filtrarViajesPorTipoTurno(List<OrderInfoResponse> viajes, 
                                                              CalculatedShift.TipoTurno tipoTurno, 
                                                              LocalDate fecha) {
        if (tipoTurno == CalculatedShift.TipoTurno.diurno) {
            // Para turnos diurnos, solo incluir viajes que terminen antes de las 18:00
            LocalDateTime limiteDiurno = fecha.atTime(HORA_FIN_DIURNO, 0, 0);
            return viajes.stream()
                .filter(v -> {
                    LocalDateTime finViaje = obtenerFechaFinViaje(v);
                    return finViaje != null && finViaje.isBefore(limiteDiurno);
                })
                .collect(Collectors.toList());
        } else if (tipoTurno == CalculatedShift.TipoTurno.nocturno) {
            // Para turnos nocturnos, incluir viajes que:
            // 1. Empiecen a las 18:00 o después
            // 2. O crucen las 18:00 (empiecen antes pero terminen después de las 18:00)
            LocalDateTime inicioNocturno = fecha.atTime(HORA_INICIO_NOCTURNO, 0, 0);
            LocalDateTime limiteDiurno = fecha.atTime(HORA_FIN_DIURNO, 0, 0);
            return viajes.stream()
                .filter(v -> {
                    LocalDateTime inicioViaje = obtenerFechaInicioViaje(v);
                    LocalDateTime finViaje = obtenerFechaFinViaje(v);
                    
                    if (inicioViaje == null) {
                        return false;
                    }
                    
                    // Si empieza a las 18:00 o después, es nocturno
                    if (!inicioViaje.isBefore(inicioNocturno)) {
                        return true;
                    }
                    
                    // Si cruza las 18:00 (termina después de las 18:00 aunque empiece antes), es nocturno
                    if (finViaje != null && finViaje.isAfter(limiteDiurno)) {
                        return true;
                    }
                    
                    return false;
                })
                .collect(Collectors.toList());
        }
        return viajes;
    }
    
    private HorasTurno crearHorasTurnoVacio() {
        return new HorasTurno(null, null, null, CalculatedShift.EstadoTurno.activo);
    }
    
    private List<OrderInfoResponse> ordenarViajesPorFechaInicio(List<OrderInfoResponse> viajes) {
        return viajes.stream()
            .filter(v -> v.getBookedAt() != null)
            .sorted(Comparator.comparing(v -> {
                try {
                    ZonedDateTime bookedAt = parsearFechaViaje(v.getBookedAt());
                    return bookedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
                } catch (Exception e) {
                    return LocalDateTime.MIN;
                }
            }))
            .collect(Collectors.toList());
    }
    
    private HorasTurno procesarViajesParaCalcularHoras(List<OrderInfoResponse> viajesOrdenados, 
                                                        CalculatedShift.TipoTurno tipoTurno, 
                                                        LocalDate fecha) {
        LocalDateTime horaInicio = null;
        LocalDateTime horaFin = null;
        LocalDateTime ultimoViajeFin = null;
        
        // Límite para turnos diurnos: 18:00
        LocalDateTime limiteDiurno = fecha.atTime(HORA_FIN_DIURNO, 0, 0);
        LocalDateTime inicioNocturno = fecha.atTime(HORA_INICIO_NOCTURNO, 0, 0);
        
        for (int i = 0; i < viajesOrdenados.size(); i++) {
            OrderInfoResponse viaje = viajesOrdenados.get(i);
            
            if (horaInicio == null) {
                LocalDateTime inicioViaje = obtenerFechaInicioViaje(viaje);
                // Para turnos nocturnos, el turno empieza a la hora de inicio del primer viaje
                // (que puede ser antes de las 18:00 si cruza las 18:00, o después de las 18:00)
                horaInicio = inicioViaje;
            }
            
            LocalDateTime finViaje = obtenerFechaFinViaje(viaje);
            if (finViaje != null) {
                ultimoViajeFin = finViaje;
            }
            
            // Para turnos diurnos, limitar a las 18:00
            if (tipoTurno == CalculatedShift.TipoTurno.diurno && finViaje != null && finViaje.isAfter(limiteDiurno)) {
                log.info("🕐 [CalculatedShiftService] Turno diurno limitado a las 18:00. Viaje termina a las {}", finViaje.toLocalTime());
                horaFin = limiteDiurno;
                break;
            }
            
            // Verificar gap de inactividad con el viaje anterior
            if (i > 0) {
                LocalDateTime finViajeAnterior = obtenerFinViaje(viajesOrdenados.get(i - 1));
                LocalDateTime inicioViajeActual = obtenerFechaInicioViaje(viaje);
                
                if (finViajeAnterior != null && inicioViajeActual != null) {
                    long minutosInactividad = Duration.between(finViajeAnterior, inicioViajeActual).toMinutes();
                    
                    if (minutosInactividad >= INACTIVIDAD_MAXIMA_MINUTOS) {
                        log.info("⏸️ [CalculatedShiftService] Detectado gap de inactividad de {} minutos ({} horas) - cerrando turno en {}", 
                            minutosInactividad, minutosInactividad / 60.0, finViajeAnterior);
                        horaFin = finViajeAnterior;
                        break;
                    }
                }
            }
            
            // Actualizar hora de fin si este viaje termina después
            if (finViaje != null && (horaFin == null || finViaje.isAfter(horaFin))) {
                horaFin = finViaje;
            }
        }
        
        // Si no se detectó un gap, usar el último viaje como fin
        if (horaFin == null && ultimoViajeFin != null) {
            horaFin = ultimoViajeFin;
        }
        
        // Asegurar que los turnos diurnos no excedan las 18:00
        if (tipoTurno == CalculatedShift.TipoTurno.diurno && horaFin != null && horaFin.isAfter(limiteDiurno)) {
            horaFin = limiteDiurno;
        }
        
        Integer duracionMinutos = calcularDuracionMinutos(horaInicio, horaFin);
        CalculatedShift.EstadoTurno estado = horaFin != null 
            ? CalculatedShift.EstadoTurno.finalizado 
            : CalculatedShift.EstadoTurno.activo;
        
        return new HorasTurno(horaInicio, horaFin, duracionMinutos, estado);
    }
    
    private LocalDateTime obtenerFechaInicioViaje(OrderInfoResponse viaje) {
        if (viaje.getBookedAt() == null) {
            return null;
        }
        try {
            ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
            return bookedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
        } catch (Exception e) {
            log.debug("Error obteniendo fecha de inicio del viaje: {}", e.getMessage());
            return null;
        }
    }
    
    private LocalDateTime obtenerFechaFinViaje(OrderInfoResponse viaje) {
        if (viaje.getEndedAt() != null && !viaje.getEndedAt().isEmpty()) {
            try {
                ZonedDateTime endedAt = parsearFechaViaje(viaje.getEndedAt());
                return endedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
            } catch (Exception e) {
                log.debug("Error obteniendo fecha de fin del viaje: {}", e.getMessage());
            }
        }
        
        // Si no hay endedAt, usar bookedAt como aproximación
        if (viaje.getBookedAt() != null && !viaje.getBookedAt().isEmpty()) {
            try {
                ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
                return bookedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
            } catch (Exception e) {
                log.debug("Error obteniendo fecha de fin del viaje (fallback): {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    private Integer calcularDuracionMinutos(LocalDateTime horaInicio, LocalDateTime horaFin) {
        if (horaInicio == null || horaFin == null) {
            return null;
        }
        return (int) Duration.between(horaInicio, horaFin).toMinutes();
    }
    
    private LocalDateTime obtenerFinViaje(OrderInfoResponse viaje) {
        if (viaje.getEndedAt() != null && !viaje.getEndedAt().isEmpty()) {
            try {
                ZonedDateTime endedAt = parsearFechaViaje(viaje.getEndedAt());
                return endedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
            } catch (Exception e) {
                return null;
            }
        } else if (viaje.getBookedAt() != null && !viaje.getBookedAt().isEmpty()) {
            // Si no hay endedAt, usar bookedAt como aproximación
            try {
                ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
                return bookedAt.withZoneSameInstant(LIMA_ZONE).toLocalDateTime();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    
    private CalculatedShift obtenerOCrearTurno(String driverId, LocalDate fecha, CalculatedShift.TipoTurno tipoTurno) {
        List<CalculatedShift> turnosExistentes = calculatedShiftRepository
            .findByDriverIdAndFecha(driverId, fecha)
            .stream()
            .filter(t -> t.getTipoTurno() == tipoTurno)
            .toList();
        
        if (turnosExistentes.isEmpty()) {
            CalculatedShift turno = new CalculatedShift();
            turno.setDriverId(driverId);
            turno.setFecha(fecha);
            log.debug("💾 [CalculatedShiftService] Creando nuevo turno {} para driver_id: {}, fecha: {}", 
                tipoTurno, driverId, fecha);
            return turno;
        } else {
            CalculatedShift turno = turnosExistentes.get(0);
            turno.setFecha(fecha);
            log.debug("🔄 [CalculatedShiftService] Actualizando turno {} existente ID: {} para driver_id: {}, fecha: {}", 
                tipoTurno, turno.getId(), driverId, fecha);
            return turno;
        }
    }
    
    private void actualizarTurno(CalculatedShift turno, CalculatedShift.TipoTurno tipoTurno, HorasTurno horasTurno, 
                                 LocalDate fecha, Double montoTotal) {
        turno.setTipoTurno(tipoTurno);
        turno.setHoraInicio(horasTurno.horaInicio);
        turno.setHoraFin(horasTurno.horaFin);
        turno.setEstado(horasTurno.estado);
        turno.setDuracionMinutos(horasTurno.duracionMinutos);
        turno.setMontoTotal(montoTotal);
        // pagado se mantiene en false por defecto (se puede actualizar manualmente después)
        
        if (!turno.getFecha().equals(fecha)) {
            log.error("❌ [CalculatedShiftService] ERROR: La fecha del turno ({}) no coincide con la esperada ({}). Corrigiendo...", 
                turno.getFecha(), fecha);
            turno.setFecha(fecha);
        }
    }
    
    private void guardarTurno(CalculatedShift turno, String driverId, LocalDate fecha, 
                             CalculatedShift.TipoTurno tipoTurno, HorasTurno horasTurno, Double montoTotal) {
        CalculatedShift saved = calculatedShiftRepository.save(turno);
        calculatedShiftRepository.flush();
        
        log.info("✅ [CalculatedShiftService] Turno {} guardado - ID: {}, driver_id: {}, fecha: {}, inicio: {}, fin: {}, duracion: {} min, monto: {} soles, pagado: {}", 
            tipoTurno, saved.getId(), driverId, fecha, horasTurno.horaInicio, horasTurno.horaFin, 
            horasTurno.duracionMinutos, montoTotal, saved.getPagado());
    }
    
    // ==================== PRIVATE METHODS: BATCH PROCESSING ====================
    
    private DriverListResponse obtenerListaConductores() {
        log.info("📋 [CalculatedShiftService] Obteniendo lista de conductores...");
        return fleetDriverService.obtenerListaConductores(null);
    }
    
    private EstadisticasProcesamiento procesarConductores(
            List<DriverListResponse.ContractorResponse> conductores, LocalDate fechaAnterior) {
        
        int totalConductores = conductores.size();
        int totalProcesados = 0;
        int totalOmitidos = 0;
        int totalErrores = 0;
        
        log.info("📊 [CalculatedShiftService] Total de conductores encontrados: {}", totalConductores);
        log.info("📊 [CalculatedShiftService] Fecha objetivo: {}", fechaAnterior);
        log.info("📊 [CalculatedShiftService] Procesando uno por uno (calcular y guardar inmediatamente)...");
        log.info("📊 [CalculatedShiftService] Delay entre conductores: {} ms (para evitar saturar API)", 
            DELAY_ENTRE_CONDUCTORES_MS);
        
        long tiempoInicio = System.currentTimeMillis();
        
        for (var contractor : conductores) {
            if (contractor.getId() == null || contractor.getId().isEmpty()) {
                log.warn("⚠️ [CalculatedShiftService] Conductor sin ID válido, omitiendo: {}", contractor.getFullName());
                continue;
            }
            
            String driverId = contractor.getId();
            String driverName = contractor.getFullName() != null ? contractor.getFullName() : "N/A";
            
            try {
                if (tieneTurnoManual(driverId, fechaAnterior)) {
                    log.info("⏭️ [CalculatedShiftService] Omitiendo conductor {} ({}) - Ya existe turno(s) manual(es) registrado(s) para fecha {}", 
                        driverName, driverId, fechaAnterior);
                    totalOmitidos++;
                    continue;
                }
                
                log.debug("🔄 [CalculatedShiftService] Procesando conductor {} ({})...", driverName, driverId);
                calcularYGuardarHorasTurno(driverId, fechaAnterior);
                totalProcesados++;
                
                aplicarDelayEntreConductores(totalProcesados, totalOmitidos, totalConductores);
                logProgresoSiEsNecesario(totalProcesados, totalConductores, totalOmitidos, totalErrores);
                
            } catch (Exception e) {
                log.error("❌ [CalculatedShiftService] Error procesando horas de turno para driver_id: {} ({}), fecha: {}: {}", 
                    driverId, driverName, fechaAnterior, e.getMessage(), e);
                totalErrores++;
                aplicarDelayEntreConductores(totalProcesados, totalOmitidos, totalConductores);
            }
        }
        
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        return new EstadisticasProcesamiento(totalProcesados, totalOmitidos, totalErrores, tiempoTotal);
    }
    
    private boolean tieneTurnoManual(String driverId, LocalDate fecha) {
        List<CalculatedShift> turnosManuales = calculatedShiftRepository
            .findByDriverIdAndFechaAndEsManual(driverId, fecha);
        return !turnosManuales.isEmpty();
    }
    
    private void aplicarDelayEntreConductores(int totalProcesados, int totalOmitidos, int totalConductores) {
        if (totalProcesados + totalOmitidos < totalConductores) {
            try {
                Thread.sleep(DELAY_ENTRE_CONDUCTORES_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ [CalculatedShiftService] Interrupción durante delay entre conductores", e);
            }
        }
    }
    
    private void logProgresoSiEsNecesario(int totalProcesados, int totalConductores, int totalOmitidos, int totalErrores) {
        if (totalProcesados % LOG_PROGRESO_CADA_N == 0) {
            log.info("📈 [CalculatedShiftService] Progreso: {}/{} conductores procesados ({} omitidos, {} errores)", 
                totalProcesados, totalConductores, totalOmitidos, totalErrores);
        }
    }
    
    // ==================== PRIVATE METHODS: RESPONSE BUILDING ====================
    
    private FechasConTiposTurnoResponse crearRespuestaVacia(String driverId) {
        return FechasConTiposTurnoResponse.builder()
            .driverId(driverId)
            .fechas(new ArrayList<>())
            .build();
    }
    
    private List<FechasConTiposTurnoResponse.FechaConTiposTurno> agruparShiftsPorFecha(List<CalculatedShift> shifts) {
        Map<LocalDate, List<CalculatedShift>> shiftsPorFecha = shifts.stream()
            .collect(Collectors.groupingBy(CalculatedShift::getFecha));
        
        return shiftsPorFecha.entrySet().stream()
            .map(entry -> {
                LocalDate fecha = entry.getKey();
                List<CalculatedShift> shiftsDeFecha = entry.getValue();
                
                List<FechasConTiposTurnoResponse.TipoTurnoInfo> tiposTurno = shiftsDeFecha.stream()
                    .map(shift -> FechasConTiposTurnoResponse.TipoTurnoInfo.builder()
                        .id(shift.getId())
                        .tipoTurno(shift.getTipoTurno().name())
                        .build())
                    .collect(Collectors.toList());
                
                return FechasConTiposTurnoResponse.FechaConTiposTurno.builder()
                    .fecha(fecha)
                    .tiposTurno(tiposTurno)
                    .build();
            })
            .sorted(Comparator.comparing(FechasConTiposTurnoResponse.FechaConTiposTurno::getFecha))
            .collect(Collectors.toList());
    }
    
    // ==================== PRIVATE METHODS: LOGGING ====================
    
    private void logInicioProcesamiento(LocalDate fechaActual, LocalDate fechaAnterior) {
        log.info("🕐 [CalculatedShiftService] ========================================");
        log.info("🕐 [CalculatedShiftService] INICIANDO PROCESAMIENTO DE TURNOS DEL DÍA ANTERIOR");
        log.info("🕐 [CalculatedShiftService] Fecha actual: {}", fechaActual);
        log.info("🕐 [CalculatedShiftService] Fecha a procesar: {}", fechaAnterior);
        log.info("🕐 [CalculatedShiftService] ========================================");
    }
    
    private void logFinProcesamiento(LocalDate fechaAnterior, EstadisticasProcesamiento stats) {
        long minutos = stats.tiempoTotal / 60000;
        long segundos = (stats.tiempoTotal % 60000) / 1000;
        
        log.info("✅ [CalculatedShiftService] ========================================");
        log.info("✅ [CalculatedShiftService] PROCESAMIENTO COMPLETADO");
        log.info("✅ [CalculatedShiftService] Fecha procesada: {}", fechaAnterior);
        log.info("✅ [CalculatedShiftService] Total procesados: {}", stats.totalProcesados);
        log.info("✅ [CalculatedShiftService] Total omitidos (turnos manuales): {}", stats.totalOmitidos);
        log.info("✅ [CalculatedShiftService] Total errores: {}", stats.totalErrores);
        log.info("✅ [CalculatedShiftService] Tiempo total: {} minutos {} segundos", minutos, segundos);
        log.info("✅ [CalculatedShiftService] ========================================");
    }
    
    private void logResumenViajes(String driverId, LocalDate fecha, ViajesDelDia viajesDelDia, 
                                  ViajesNocturnos viajesNocturnos) {
        log.info("📊 [CalculatedShiftService] Viajes filtrados para driver_id {} - fecha {}:", driverId, fecha);
        log.info("   📅 Diurnos: {} viajes (con inicio entre 5 AM - 5:59 PM del día {})", 
            viajesDelDia.viajesDiurnos.size(), fecha);
        log.info("   🌙 Nocturnos día anterior ({}): {} viajes (6 PM del {} - 4:59 AM del {})", 
            viajesNocturnos.fechaAnterior, viajesNocturnos.viajesNocturnosAnterior.size(), 
            viajesNocturnos.fechaAnterior, fecha);
        log.info("   🌙 Nocturnos día actual ({}): {} viajes (6 PM del {} - 4:59 AM del {})", 
            fecha, viajesNocturnos.viajesNocturnosActual.size(), fecha, viajesNocturnos.fechaSiguiente);
        
        logDetalleHorasViajes(viajesDelDia, viajesNocturnos);
    }
    
    private void logDetalleHorasViajes(ViajesDelDia viajesDelDia, ViajesNocturnos viajesNocturnos) {
        if (!viajesDelDia.viajesDiurnos.isEmpty()) {
            LocalDateTime primerDiurno = obtenerPrimerViaje(viajesDelDia.viajesDiurnos);
            LocalDateTime ultimoDiurno = obtenerUltimoViaje(viajesDelDia.viajesDiurnos);
            log.info("   📅 Turno diurno: primer viaje {} - último viaje {}", primerDiurno, ultimoDiurno);
        }
        if (!viajesNocturnos.viajesNocturnosAnterior.isEmpty()) {
            LocalDateTime primerNocturnoAnterior = obtenerPrimerViaje(viajesNocturnos.viajesNocturnosAnterior);
            LocalDateTime ultimoNocturnoAnterior = obtenerUltimoViaje(viajesNocturnos.viajesNocturnosAnterior);
            log.info("   🌙 Turno nocturno día anterior: primer viaje {} - último viaje {}", 
                primerNocturnoAnterior, ultimoNocturnoAnterior);
        }
        if (!viajesNocturnos.viajesNocturnosActual.isEmpty()) {
            LocalDateTime primerNocturnoActual = obtenerPrimerViaje(viajesNocturnos.viajesNocturnosActual);
            LocalDateTime ultimoNocturnoActual = obtenerUltimoViaje(viajesNocturnos.viajesNocturnosActual);
            log.info("   🌙 Turno nocturno día actual: primer viaje {} - último viaje {}", 
                primerNocturnoActual, ultimoNocturnoActual);
        }
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class ViajesDelDia {
        final List<OrderInfoResponse> viajesMadrugada;
        final List<OrderInfoResponse> viajesDiurnos;
        
        ViajesDelDia(List<OrderInfoResponse> viajesMadrugada, List<OrderInfoResponse> viajesDiurnos) {
            this.viajesMadrugada = viajesMadrugada;
            this.viajesDiurnos = viajesDiurnos;
        }
    }
    
    private static class ViajesNocturnos {
        final List<OrderInfoResponse> viajesNocturnosAnterior;
        final List<OrderInfoResponse> viajesNocturnosActual;
        final LocalDate fechaAnterior;
        final LocalDate fechaSiguiente;
        
        ViajesNocturnos(List<OrderInfoResponse> viajesNocturnosAnterior, 
                       List<OrderInfoResponse> viajesNocturnosActual,
                       LocalDate fechaAnterior, LocalDate fechaSiguiente) {
            this.viajesNocturnosAnterior = viajesNocturnosAnterior;
            this.viajesNocturnosActual = viajesNocturnosActual;
            this.fechaAnterior = fechaAnterior;
            this.fechaSiguiente = fechaSiguiente;
        }
    }
    
    private static class HorasTurno {
        final LocalDateTime horaInicio;
        final LocalDateTime horaFin;
        final Integer duracionMinutos;
        final CalculatedShift.EstadoTurno estado;
        
        HorasTurno(LocalDateTime horaInicio, LocalDateTime horaFin, Integer duracionMinutos, 
                   CalculatedShift.EstadoTurno estado) {
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
            this.duracionMinutos = duracionMinutos;
            this.estado = estado;
        }
    }
    
    private static class EstadisticasProcesamiento {
        final int totalProcesados;
        final int totalOmitidos;
        final int totalErrores;
        final long tiempoTotal;
        
        EstadisticasProcesamiento(int totalProcesados, int totalOmitidos, int totalErrores, long tiempoTotal) {
            this.totalProcesados = totalProcesados;
            this.totalOmitidos = totalOmitidos;
            this.totalErrores = totalErrores;
            this.tiempoTotal = tiempoTotal;
        }
    }
}
