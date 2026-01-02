package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftManualRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.TurnoRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.AllDriversOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.CalculatedShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculatedShiftServiceImpl implements CalculatedShiftService {

    private final CalculatedShiftRepository calculatedShiftRepository;
    private final DriverOrdersService driverOrdersService;
    private final FleetDriverService fleetDriverService;
    private static final ZoneId ZONE_UTC_MINUS_5 = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_MANUAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATETIME_MANUAL_FORMATTER_NO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FECHA_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    @Transactional
    public CalculatedShiftResponse guardarTurnos(CalculatedShiftRequest request) {
        LocalDate fecha = LocalDate.parse(request.getFecha(), DATE_FORMATTER);
        List<CalculatedShiftResponse.TurnoResponse> turnosGuardados = new ArrayList<>();

        for (TurnoRequest turnoRequest : request.getTurnos()) {
            try {
                CalculatedShift turno = procesarTurno(request.getDriverId(), fecha, turnoRequest);
                CalculatedShift turnoGuardado = calculatedShiftRepository.save(turno);
                turnosGuardados.add(mapearATurnoResponse(turnoGuardado));
            } catch (Exception e) {
                log.error("❌ Error procesando turno: {}", e.getMessage(), e);
            }
        }

        return CalculatedShiftResponse.builder()
            .driverId(request.getDriverId())
            .fecha(request.getFecha())
            .turnos(turnosGuardados)
            .build();
    }

    @Override
    public CalculatedShiftResponse listarTurnos(String driverId, String fecha) {
        List<CalculatedShift> turnos = calculatedShiftRepository.findByDriverIdAndFecha(
            driverId, LocalDate.parse(fecha, DATE_FORMATTER));
        
        return CalculatedShiftResponse.builder()
            .driverId(driverId)
            .fecha(fecha)
            .turnos(turnos.stream().map(this::mapearATurnoResponse).toList())
            .build();
    }

    private CalculatedShift procesarTurno(String driverId, LocalDate fecha, TurnoRequest turnoRequest) {
        LocalDateTime horaInicio = ZonedDateTime.parse(turnoRequest.getHoraInicio(), DATETIME_FORMATTER)
            .withZoneSameInstant(ZONE_UTC_MINUS_5).toLocalDateTime();
        LocalDateTime horaFin = turnoRequest.getHoraFin() != null && !turnoRequest.getHoraFin().isEmpty()
            ? ZonedDateTime.parse(turnoRequest.getHoraFin(), DATETIME_FORMATTER)
                .withZoneSameInstant(ZONE_UTC_MINUS_5).toLocalDateTime()
            : null;

        return calculatedShiftRepository
            .findByDriverIdAndFechaAndHoraInicio(driverId, fecha, horaInicio)
            .map(turno -> actualizarTurnoExistente(turno, turnoRequest, horaFin))
            .orElseGet(() -> crearNuevoTurno(driverId, fecha, turnoRequest, horaInicio, horaFin));
    }

    private CalculatedShift actualizarTurnoExistente(CalculatedShift turno, TurnoRequest request, LocalDateTime horaFin) {
        if (turno.getEstado() == CalculatedShift.EstadoTurno.activo && horaFin != null) {
            turno.setHoraFin(horaFin);
            turno.setEstado(CalculatedShift.EstadoTurno.finalizado);
            turno.setDuracionMinutos((int) Duration.between(turno.getHoraInicio(), horaFin).toMinutes());
        }
        if (request.getTipoTurno() != null) {
            turno.setTipoTurno(CalculatedShift.TipoTurno.valueOf(request.getTipoTurno()));
        }
        if (request.getEstado() != null) {
            turno.setEstado(CalculatedShift.EstadoTurno.valueOf(request.getEstado()));
        }
        if (request.getDuracionMinutos() != null) {
            turno.setDuracionMinutos(request.getDuracionMinutos());
        }
        return turno;
    }

    private CalculatedShift crearNuevoTurno(String driverId, LocalDate fecha, TurnoRequest request, 
                                           LocalDateTime horaInicio, LocalDateTime horaFin) {
        CalculatedShift turno = CalculatedShift.builder()
            .driverId(driverId)
            .fecha(fecha)
            .horaInicio(horaInicio)
            .horaFin(horaFin)
            .tipoTurno(CalculatedShift.TipoTurno.valueOf(request.getTipoTurno()))
            .estado(horaFin != null ? CalculatedShift.EstadoTurno.finalizado : CalculatedShift.EstadoTurno.activo)
            .duracionMinutos(horaFin != null 
                ? (int) Duration.between(horaInicio, horaFin).toMinutes()
                : request.getDuracionMinutos())
            .build();
        
        if (request.getEstado() != null) {
            turno.setEstado(CalculatedShift.EstadoTurno.valueOf(request.getEstado()));
        }
        return turno;
    }

    private CalculatedShiftResponse.TurnoResponse mapearATurnoResponse(CalculatedShift turno) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        
        return CalculatedShiftResponse.TurnoResponse.builder()
            .id(turno.getId())
            .horaInicio(turno.getHoraInicio().atZone(ZONE_UTC_MINUS_5).format(formatter))
            .horaFin(turno.getHoraFin() != null 
                ? turno.getHoraFin().atZone(ZONE_UTC_MINUS_5).format(formatter) 
                : null)
            .tipoTurno(turno.getTipoTurno().name())
            .estado(turno.getEstado().name())
            .duracionMinutos(turno.getDuracionMinutos())
            .build();
    }

    @Override
    @Transactional
    public void actualizarTurnoConductor(String driverId, String status, Integer statusDuration, LocalDateTime primeraVezVistoActivoHoy) {
        if (driverId == null || status == null || statusDuration == null) {
            return;
        }
        
        LocalDate fechaActual = LocalDate.now(ZONE_UTC_MINUS_5);
        LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
        
        if ("offline".equals(status)) {
            finalizarTurnosActivos(driverId, fechaActual);
            finalizarTurnosActivos(driverId, fechaActual.minusDays(1));
            return;
        }
        
        CalculatedShift turnoActivoHoy = buscarTurnoActivo(driverId, fechaActual);
        if (turnoActivoHoy != null) {
            verificarYFinalizarSiInactivo(turnoActivoHoy, ahora);
            return;
        }
        
        CalculatedShift turnoActivoDiaAnterior = buscarTurnoActivo(driverId, fechaActual.minusDays(1));
        if (turnoActivoDiaAnterior != null) {
            verificarYFinalizarSiInactivo(turnoActivoDiaAnterior, ahora);
            return;
        }
        
    }
    
    private CalculatedShift buscarTurnoActivo(String driverId, LocalDate fecha) {
        return calculatedShiftRepository
            .findActiveShiftsByDriverIdAndFecha(driverId, fecha)
            .stream()
            .filter(t -> t.getEstado() == CalculatedShift.EstadoTurno.activo && t.getHoraFin() == null)
            .findFirst()
            .orElse(null);
    }

    @Override
    @Transactional
    public void finalizarTurnosActivos(String driverId, LocalDate fecha) {
        List<CalculatedShift> turnosActivos = calculatedShiftRepository
            .findByDriverIdAndFechaAndEstado(driverId, fecha, CalculatedShift.EstadoTurno.activo);
        
        if (turnosActivos.isEmpty()) {
            return;
        }
        
        LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
        
        for (CalculatedShift turno : turnosActivos) {
            if (turno.getHoraFin() == null) {
                turno.setHoraFin(ahora);
                turno.setEstado(CalculatedShift.EstadoTurno.finalizado);
                turno.setDuracionMinutos((int) Duration.between(turno.getHoraInicio(), ahora).toMinutes());
                calculatedShiftRepository.save(turno);
            }
        }
    }
    
    @Override
    @Transactional
    public void verificarYFinalizarTurnosDesconectados(java.util.Set<String> conductoresActivos) {
        LocalDate fechaActual = LocalDate.now(ZONE_UTC_MINUS_5);
        LocalDateTime ahora = LocalDateTime.now(ZONE_UTC_MINUS_5);
        
        procesarTurnosInactivos(conductoresActivos, fechaActual, ahora);
        procesarTurnosInactivos(conductoresActivos, fechaActual.minusDays(1), ahora);
    }
    
    private void procesarTurnosInactivos(java.util.Set<String> conductoresActivos, LocalDate fecha, LocalDateTime ahora) {
        calculatedShiftRepository
            .findActiveShiftsByFecha(CalculatedShift.EstadoTurno.activo, fecha)
            .stream()
            .filter(t -> t.getEstado() == CalculatedShift.EstadoTurno.activo && t.getHoraFin() == null)
            .filter(t -> !conductoresActivos.contains(t.getDriverId()))
            .forEach(turno -> verificarYFinalizarSiInactivo(turno, ahora));
    }

    private LocalDateTime obtenerPrimeraOrdenDelDia(String driverId, LocalDate fecha) {
        try {
            DriverOrdersResponse response = driverOrdersService.obtenerOrdenesDelDia(
                DriverOrdersRequest.builder().driverId(driverId).build());
            
            if (response == null || response.getOrders() == null || response.getOrders().isEmpty()) {
                return null;
            }
            
            return response.getOrders().stream()
                .filter(order -> order.getBookedAt() != null && !order.getBookedAt().isEmpty())
                .map(order -> parseDate(order.getBookedAt()))
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, ORDER_DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void verificarYFinalizarSiInactivo(CalculatedShift turno, LocalDateTime ahora) {
        LocalDateTime ultimoViaje = obtenerUltimoViajeDelDia(turno.getDriverId());
        if (ultimoViaje == null || Duration.between(ultimoViaje, ahora).toHours() <= 2) {
            return;
        }
        
        LocalDateTime horaFin = ultimoViaje.plusHours(2);
        turno.setHoraFin(horaFin);
        turno.setEstado(CalculatedShift.EstadoTurno.finalizado);
        turno.setDuracionMinutos((int) Duration.between(turno.getHoraInicio(), horaFin).toMinutes());
        calculatedShiftRepository.save(turno);
    }

    private LocalDateTime obtenerUltimoViajeDelDia(String driverId) {
        try {
            DriverOrdersResponse response = driverOrdersService.obtenerOrdenesDelDia(
                DriverOrdersRequest.builder().driverId(driverId).build());
            
            if (response == null || response.getOrders() == null || response.getOrders().isEmpty()) {
                return null;
            }
            
            return response.getOrders().stream()
                .map(this::obtenerFechaViaje)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
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
    

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calcularYGuardarHorasTurno(String driverId, LocalDate fecha) {
        try {
            // Validar que la fecha no sea null
            if (fecha == null) {
                log.error("❌ [CalculatedShiftService] Fecha es null para driver_id: {}", driverId);
                return;
            }
            
            log.info("🕐 [CalculatedShiftService] Calculando horas de turno para driver_id: {}, fecha: {}", driverId, fecha);
            
            DateTimeFormatter apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            
            // 1. CONSULTAR VIAJES DIURNOS: del día actual desde 5:00 AM hasta 5:59 PM
            ZonedDateTime inicioDiurno = fecha.atTime(5, 0, 0).atZone(ZONE_UTC_MINUS_5);
            ZonedDateTime finDiurno = fecha.atTime(17, 59, 59).atZone(ZONE_UTC_MINUS_5);
            String dateFromDiurno = inicioDiurno.format(apiDateFormatter);
            String dateToDiurno = finDiurno.format(apiDateFormatter);
            
            DriverOrdersResponse respuestaDiurno = driverOrdersService.obtenerViajesCompletos(driverId, dateFromDiurno, dateToDiurno, null);
            List<OrderInfoResponse> todosViajesDiurnos = respuestaDiurno.getOrders() != null ? respuestaDiurno.getOrders() : new ArrayList<>();
            
            // FILTRAR: Solo incluir viajes cuya hora de INICIO (bookedAt) esté entre 5 AM - 5:59 PM
            List<OrderInfoResponse> viajesDiurnos = new ArrayList<>();
            for (OrderInfoResponse viaje : todosViajesDiurnos) {
                if (viaje.getBookedAt() != null) {
                    try {
                        ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
                        LocalDateTime bookedDateTime = bookedAt.withZoneSameInstant(ZONE_UTC_MINUS_5).toLocalDateTime();
                        int hora = bookedDateTime.getHour();
                        // Diurno: 5:00 AM - 5:59 PM (5 <= hora < 18)
                        if (hora >= 5 && hora < 18) {
                            viajesDiurnos.add(viaje);
                        }
                    } catch (Exception e) {
                        log.debug("Error filtrando viaje diurno: {}", e.getMessage());
                    }
                }
            }
            
            // 2. CONSULTAR VIAJES NOCTURNOS: desde las 6:00 PM del día anterior hasta las 4:59 AM del día actual
            // NOTA: Las 6 PM del día anterior es solo el límite de referencia. El turno nocturno puede empezar
            // a cualquier hora después de las 6 PM (10 PM, 11 PM, 00:10 AM, etc.). La hora de inicio real
            // se determina por el primer viaje encontrado en este rango.
            LocalDate fechaAnterior = fecha.minusDays(1);
            ZonedDateTime inicioNocturno = fechaAnterior.atTime(18, 0, 0).atZone(ZONE_UTC_MINUS_5);
            ZonedDateTime finNocturno = fecha.atTime(4, 59, 59).atZone(ZONE_UTC_MINUS_5);
            String dateFromNocturno = inicioNocturno.format(apiDateFormatter);
            String dateToNocturno = finNocturno.format(apiDateFormatter);
            
            DriverOrdersResponse respuestaNocturno = driverOrdersService.obtenerViajesCompletos(driverId, dateFromNocturno, dateToNocturno, null);
            List<OrderInfoResponse> todosViajesNocturnos = respuestaNocturno.getOrders() != null ? respuestaNocturno.getOrders() : new ArrayList<>();
            
            // FILTRAR: Solo incluir viajes cuya hora de INICIO (bookedAt) esté entre 6 PM - 4:59 AM
            List<OrderInfoResponse> viajesNocturnos = new ArrayList<>();
            for (OrderInfoResponse viaje : todosViajesNocturnos) {
                if (viaje.getBookedAt() != null) {
                    try {
                        ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
                        LocalDateTime bookedDateTime = bookedAt.withZoneSameInstant(ZONE_UTC_MINUS_5).toLocalDateTime();
                        int hora = bookedDateTime.getHour();
                        // Nocturno: 6:00 PM - 4:59 AM (hora >= 18 || hora < 5)
                        if (hora >= 18 || hora < 5) {
                            viajesNocturnos.add(viaje);
                        }
                    } catch (Exception e) {
                        log.debug("Error filtrando viaje nocturno: {}", e.getMessage());
                    }
                }
            }
            
            log.info("📊 [CalculatedShiftService] Viajes filtrados - Diurnos: {} (con inicio entre 5 AM - 5:59 PM del día {}), Nocturnos: {} (con inicio entre 6 PM del {} - 4:59 AM del {})", 
                viajesDiurnos.size(), fecha, viajesNocturnos.size(), fechaAnterior, fecha);
            
            // 3. Procesar turno diurno si hay viajes (se guarda con fecha del día actual)
            if (!viajesDiurnos.isEmpty()) {
                procesarTurno(driverId, fecha, viajesDiurnos, CalculatedShift.TipoTurno.diurno);
            }
            
            // 4. Procesar turno nocturno si hay viajes
            // Se guarda con fecha del día anterior (porque el turno pertenece al día anterior aunque pueda extenderse hasta la madrugada)
            // La hora de inicio real será la del primer viaje encontrado (puede ser 10 PM, 11 PM, 00:10 AM, etc.)
            if (!viajesNocturnos.isEmpty()) {
                procesarTurno(driverId, fechaAnterior, viajesNocturnos, CalculatedShift.TipoTurno.nocturno);
            }
            
            if (viajesDiurnos.isEmpty() && viajesNocturnos.isEmpty()) {
                log.warn("⚠️ [CalculatedShiftService] No se encontraron viajes para driver_id: {}, fecha: {}", driverId, fecha);
            }
            
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error calculando horas de turno para driver_id: {}, fecha: {}: {}", 
                driverId, fecha, e.getMessage(), e);
        }
    }

    @Override
    public void procesarHorasTurnoDiaAnterior() {
        LocalDate fechaActual = LocalDate.now(ZONE_UTC_MINUS_5);
        LocalDate fechaAnterior = fechaActual.minusDays(1);
        log.info("🕐 [CalculatedShiftService] Procesando horas de turno del día anterior. Fecha actual: {}, Fecha a procesar: {}", 
            fechaActual, fechaAnterior);
        
        try {
            // Obtener lista de conductores (sin filtro, obtener todos)
            var listaConductores = fleetDriverService.obtenerListaConductores(null);
            
            if (listaConductores == null || listaConductores.getContractors() == null || listaConductores.getContractors().isEmpty()) {
                log.warn("⚠️ [CalculatedShiftService] No se encontraron conductores para procesar");
                return;
            }
            
            int totalConductores = listaConductores.getContractors().size();
            int totalProcesados = 0;
            int totalErrores = 0;
            
            log.info("📊 [CalculatedShiftService] Total de conductores a procesar: {}, Fecha objetivo: {}. Procesando uno por uno (calcular y guardar inmediatamente)...", 
                totalConductores, fechaAnterior);
            
            // Procesar uno por uno: calcular TODO y guardar inmediatamente antes de pasar al siguiente
            for (var contractor : listaConductores.getContractors()) {
                if (contractor.getId() == null || contractor.getId().isEmpty()) {
                    continue;
                }
                
                try {
                    // Verificar si ya existe un turno manual para este conductor y fecha
                    List<CalculatedShift> turnosManuales = calculatedShiftRepository
                        .findByDriverIdAndFechaAndEsManual(contractor.getId(), fechaAnterior);
                    
                    if (!turnosManuales.isEmpty()) {
                        log.info("⏭️ [CalculatedShiftService] Omitiendo conductor {} - Ya existe turno manual registrado para fecha {}", 
                            contractor.getId(), fechaAnterior);
                        totalProcesados++; // Contamos como procesado (aunque se omitió)
                        continue;
                    }
                    
                    // Calcular TODO y guardar inmediatamente (uno por uno)
                    calcularYGuardarHorasTurno(contractor.getId(), fechaAnterior);
                    totalProcesados++;
                    
                    // Delay adicional entre conductores para distribuir mejor las requests y evitar 429
                    // Esto permite que los proxies se distribuyan mejor y reduce la carga en la API
                    if (totalProcesados < totalConductores) {
                        try {
                            Thread.sleep(2000); // 2 segundos adicionales entre cada conductor
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("⚠️ [CalculatedShiftService] Interrupción durante delay entre conductores", e);
                        }
                    }
                    
                    // Log cada 5 conductores procesados para seguimiento
                    if (totalProcesados % 5 == 0) {
                        log.info("📈 [CalculatedShiftService] Progreso: {}/{} conductores procesados y guardados", totalProcesados, totalConductores);
                    }
                } catch (Exception e) {
                    log.error("❌ [CalculatedShiftService] Error procesando horas de turno para driver_id: {}, fecha: {}: {}", 
                        contractor.getId(), fechaAnterior, e.getMessage(), e);
                    totalErrores++;
                    
                    // Delay incluso en caso de error para no saturar la API
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            log.info("✅ [CalculatedShiftService] PROCESAMIENTO COMPLETADO - Total procesados: {}, Errores: {}, Fecha procesada: {}", 
                totalProcesados, totalErrores, fechaAnterior);
            
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error en procesamiento de horas de turno del día anterior (fecha: {}): {}", 
                fechaAnterior, e.getMessage(), e);
        }
    }
    
    /**
     * Procesa y guarda un turno (diurno o nocturno) basado en los viajes proporcionados
     * 
     * Para turnos nocturnos: La hora de inicio real será la del primer viaje encontrado
     * (puede ser 10 PM, 11 PM, 00:10 AM, etc.), no necesariamente las 6 PM.
     * Las 6 PM del día anterior es solo el límite de referencia para consultar.
     */
    private void procesarTurno(String driverId, LocalDate fecha, List<OrderInfoResponse> viajes, CalculatedShift.TipoTurno tipoTurno) {
        if (viajes.isEmpty()) {
            return;
        }
        
        // Encontrar hora de inicio REAL (primer booked_at de los viajes) y hora de fin (último ended_at)
        LocalDateTime horaInicio = null;
        LocalDateTime horaFin = null;
        
        for (OrderInfoResponse viaje : viajes) {
            try {
                if (viaje.getBookedAt() != null) {
                    ZonedDateTime bookedAt = parsearFechaViaje(viaje.getBookedAt());
                    LocalDateTime bookedDateTime = bookedAt.withZoneSameInstant(ZONE_UTC_MINUS_5).toLocalDateTime();
                    if (horaInicio == null || bookedDateTime.isBefore(horaInicio)) {
                        horaInicio = bookedDateTime;
                    }
                }
                
                if (viaje.getEndedAt() != null) {
                    ZonedDateTime endedAt = parsearFechaViaje(viaje.getEndedAt());
                    LocalDateTime endedDateTime = endedAt.withZoneSameInstant(ZONE_UTC_MINUS_5).toLocalDateTime();
                    if (horaFin == null || endedDateTime.isAfter(horaFin)) {
                        horaFin = endedDateTime;
                    }
                }
            } catch (Exception e) {
                log.debug("Error parseando fecha del viaje: {}", e.getMessage());
            }
        }
        
        if (horaInicio == null) {
            log.warn("⚠️ [CalculatedShiftService] No se pudo determinar hora de inicio para turno {} - driver_id: {}, fecha: {}", 
                tipoTurno, driverId, fecha);
            return;
        }
        
        // Calcular duración y estado
        Integer duracionMinutos = null;
        CalculatedShift.EstadoTurno estado;
        if (horaFin != null) {
            duracionMinutos = (int) Duration.between(horaInicio, horaFin).toMinutes();
            estado = CalculatedShift.EstadoTurno.finalizado;
        } else {
            estado = CalculatedShift.EstadoTurno.activo;
        }
        
        // Buscar si ya existe un turno de este tipo para este driver y fecha
        List<CalculatedShift> turnosExistentes = calculatedShiftRepository
            .findByDriverIdAndFecha(driverId, fecha)
            .stream()
            .filter(t -> t.getTipoTurno() == tipoTurno)
            .toList();
        
        CalculatedShift turno;
        boolean esNuevo = turnosExistentes.isEmpty();
        
        if (esNuevo) {
            // Crear nuevo registro
            turno = new CalculatedShift();
            turno.setDriverId(driverId);
            turno.setFecha(fecha);
            log.debug("💾 [CalculatedShiftService] Creando nuevo turno {} para driver_id: {}, fecha: {}", 
                tipoTurno, driverId, fecha);
        } else {
            // Actualizar el registro existente del mismo tipo
            turno = turnosExistentes.get(0);
            turno.setFecha(fecha);
            log.debug("🔄 [CalculatedShiftService] Actualizando turno {} existente ID: {} para driver_id: {}, fecha: {}", 
                tipoTurno, turno.getId(), driverId, fecha);
        }
        
        // Establecer todos los campos
        turno.setTipoTurno(tipoTurno);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFin(horaFin);
        turno.setEstado(estado);
        turno.setDuracionMinutos(duracionMinutos);
        
        // Verificar que la fecha sea la correcta ANTES de guardar
        if (!turno.getFecha().equals(fecha)) {
            log.error("❌ [CalculatedShiftService] ERROR: La fecha del turno ({}) no coincide con la esperada ({}). Corrigiendo...", 
                turno.getFecha(), fecha);
            turno.setFecha(fecha);
        }
        
        // Guardar y hacer flush
        CalculatedShift saved = calculatedShiftRepository.save(turno);
        calculatedShiftRepository.flush();
        
        log.info("✅ [CalculatedShiftService] Turno {} guardado - ID: {}, driver_id: {}, fecha: {}, inicio: {}, fin: {}, duracion: {} min", 
            tipoTurno, saved.getId(), driverId, fecha, horaInicio, horaFin, duracionMinutos);
    }
    
    /**
     * Determina si una hora pertenece al turno diurno o nocturno
     * Diurno: 5:00 AM - 5:59 PM (5 <= hora < 18)
     * Nocturno: 6:00 PM - 4:59 AM (hora >= 18 || hora < 5)
     */
    private CalculatedShift.TipoTurno determinarTipoTurno(int hora) {
        if (hora >= 5 && hora < 18) {
            return CalculatedShift.TipoTurno.diurno;
        } else {
            return CalculatedShift.TipoTurno.nocturno;
        }
    }
    
    /**
     * Parsea una fecha en formato ISO a ZonedDateTime
     * Maneja diferentes formatos: con zona horaria, sin zona horaria, con milisegundos, etc.
     */
    private ZonedDateTime parsearFechaViaje(String fechaStr) {
        try {
            // Intentar parseo ISO estándar (con zona horaria)
            return ZonedDateTime.parse(fechaStr);
        } catch (Exception e) {
            try {
                // Intentar parseo sin zona horaria - asumir UTC-5 (America/Lima)
                LocalDateTime localDateTime = LocalDateTime.parse(fechaStr, ORDER_DATE_FORMATTER);
                return localDateTime.atZone(ZONE_UTC_MINUS_5);
            } catch (Exception e2) {
                try {
                    // Intentar con formato que incluye milisegundos
                    DateTimeFormatter formatterWithMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    LocalDateTime localDateTime = LocalDateTime.parse(fechaStr, formatterWithMillis);
                    return localDateTime.atZone(ZONE_UTC_MINUS_5);
                } catch (Exception e3) {
                    log.error("❌ [CalculatedShiftService] Error parseando fecha: {}", fechaStr);
                    throw new RuntimeException("Error parseando fecha: " + fechaStr, e3);
                }
            }
        }
    }

    @Override
    @Transactional
    public CalculatedShift guardarTurnoManual(CalculatedShiftManualRequest request) {
        try {
            log.info("📝 [CalculatedShiftService] Guardando turno manual para driverId: {}, fecha: {}, horaInicio: {}, horaFin: {}", 
                request.getDriverId(), request.getFecha(), request.getHoraInicio(), request.getHoraFin());
            
            // Parsear fecha - puede venir como "yyyy-MM-dd" o "yyyy-MM-dd'T'HH:mm"
            LocalDate fecha;
            try {
                // Intentar parsear como fecha con hora (formato "yyyy-MM-dd'T'HH:mm")
                LocalDateTime fechaDateTime = LocalDateTime.parse(request.getFecha(), FECHA_DATETIME_FORMATTER);
                fecha = fechaDateTime.toLocalDate();
            } catch (Exception e) {
                // Si falla, intentar parsear solo como fecha
                try {
                    fecha = LocalDate.parse(request.getFecha(), DATE_FORMATTER);
                } catch (Exception e2) {
                    throw new IllegalArgumentException("Formato de fecha inválido: " + request.getFecha() + ". Use 'yyyy-MM-dd' o 'yyyy-MM-dd'T'HH:mm'");
                }
            }
            
            // Parsear horaInicio - puede venir en formato "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm" o solo "HH:mm"
            LocalDateTime horaInicio;
            try {
                // Intentar parsear como fecha y hora completa con segundos
                horaInicio = LocalDateTime.parse(request.getHoraInicio(), DATETIME_MANUAL_FORMATTER);
            } catch (Exception e) {
                try {
                    // Intentar parsear como fecha y hora sin segundos (formato del frontend)
                    horaInicio = LocalDateTime.parse(request.getHoraInicio(), DATETIME_MANUAL_FORMATTER_NO_SECONDS);
                } catch (Exception e2) {
                    // Si falla, intentar parsear solo como hora y combinarla con la fecha
                    try {
                        LocalTime horaInicioTime = LocalTime.parse(request.getHoraInicio(), TIME_FORMATTER);
                        horaInicio = fecha.atTime(horaInicioTime);
                    } catch (Exception e3) {
                        throw new IllegalArgumentException("Formato de horaInicio inválido: " + request.getHoraInicio() + ". Use 'yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd HH:mm' o 'HH:mm'");
                    }
                }
            }
            
            // Parsear horaFin - puede venir en formato "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm" o solo "HH:mm" o ser null
            LocalDateTime horaFin = null;
            if (request.getHoraFin() != null && !request.getHoraFin().isEmpty()) {
                try {
                    // Intentar parsear como fecha y hora completa con segundos
                    horaFin = LocalDateTime.parse(request.getHoraFin(), DATETIME_MANUAL_FORMATTER);
                } catch (Exception e) {
                    try {
                        // Intentar parsear como fecha y hora sin segundos (formato del frontend)
                        horaFin = LocalDateTime.parse(request.getHoraFin(), DATETIME_MANUAL_FORMATTER_NO_SECONDS);
                    } catch (Exception e2) {
                        // Si falla, intentar parsear solo como hora y combinarla con la fecha
                        try {
                            LocalTime horaFinTime = LocalTime.parse(request.getHoraFin(), TIME_FORMATTER);
                            horaFin = fecha.atTime(horaFinTime);
                        } catch (Exception e3) {
                            throw new IllegalArgumentException("Formato de horaFin inválido: " + request.getHoraFin() + ". Use 'yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd HH:mm' o 'HH:mm'");
                        }
                    }
                }
            }
            
            // Validar y determinar tipo de turno
            CalculatedShift.TipoTurno tipoTurno;
            if (request.getTipoTurno() != null && !request.getTipoTurno().isEmpty()) {
                try {
                    tipoTurno = CalculatedShift.TipoTurno.valueOf(request.getTipoTurno().toLowerCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Tipo de turno inválido: " + request.getTipoTurno() + ". Debe ser 'diurno' o 'nocturno'");
                }
            } else {
                // Si no se especifica, determinar automáticamente basado en la hora de inicio
                tipoTurno = determinarTipoTurno(horaInicio.getHour());
                log.info("📝 [CalculatedShiftService] Tipo de turno determinado automáticamente: {} (hora inicio: {})", 
                    tipoTurno, horaInicio.getHour());
            }
            
            // Calcular duración si hay hora fin
            Integer duracionMinutos = null;
            CalculatedShift.EstadoTurno estado;
            if (horaFin != null) {
                duracionMinutos = (int) Duration.between(horaInicio, horaFin).toMinutes();
                estado = CalculatedShift.EstadoTurno.finalizado;
            } else {
                estado = CalculatedShift.EstadoTurno.activo;
            }
            
            // Buscar si ya existe un turno del mismo tipo para este conductor y fecha
            List<CalculatedShift> turnosExistentes = calculatedShiftRepository
                .findByDriverIdAndFecha(request.getDriverId(), fecha)
                .stream()
                .filter(t -> t.getTipoTurno() == tipoTurno)
                .toList();
            
            CalculatedShift turno;
            if (!turnosExistentes.isEmpty()) {
                // Actualizar el turno existente del mismo tipo
                turno = turnosExistentes.get(0);
                log.info("🔄 [CalculatedShiftService] Actualizando turno {} existente ID: {} para driverId: {}, fecha: {}", 
                    tipoTurno, turno.getId(), request.getDriverId(), fecha);
            } else {
                // Crear nuevo turno (puede haber otro turno del tipo opuesto en el mismo día)
                turno = new CalculatedShift();
                turno.setDriverId(request.getDriverId());
                log.info("💾 [CalculatedShiftService] Creando nuevo turno manual {} para driverId: {}, fecha: {}", 
                    tipoTurno, request.getDriverId(), fecha);
            }
            
            // Establecer todos los campos
            turno.setFecha(fecha);
            turno.setHoraInicio(horaInicio);
            turno.setHoraFin(horaFin);
            turno.setTipoTurno(tipoTurno);
            turno.setEstado(estado);
            turno.setDuracionMinutos(duracionMinutos);
            turno.setEsManual(true); // Marcar como turno manual
            
            CalculatedShift saved = calculatedShiftRepository.save(turno);
            calculatedShiftRepository.flush();
            
            log.info("✅ [CalculatedShiftService] Turno manual guardado exitosamente - ID: {}, driverId: {}, fecha: {}, tipo: {}, inicio: {}, fin: {}", 
                saved.getId(), request.getDriverId(), fecha, tipoTurno, horaInicio, horaFin);
            
            return saved;
            
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error guardando turno manual para driverId: {}, fecha: {}: {}", 
                request.getDriverId(), request.getFecha(), e.getMessage(), e);
            throw new RuntimeException("Error al guardar turno manual: " + e.getMessage(), e);
        }
    }
    
    @Override
    public AllDriversOrdersResponse listarTodosLosConductoresConViajes(String fecha) {
        log.info("🚗 [CalculatedShiftService] Listando todos los conductores con sus viajes en vivo");
        
        try {
            // Determinar la fecha a usar (si es null, usar fecha actual)
            LocalDate fechaConsulta;
            if (fecha != null && !fecha.isEmpty()) {
                fechaConsulta = LocalDate.parse(fecha, DATE_FORMATTER);
            } else {
                fechaConsulta = LocalDate.now(ZONE_UTC_MINUS_5);
            }
            
            // Calcular rango de fechas para la consulta (todo el día)
            LocalDateTime inicioDia = fechaConsulta.atStartOfDay();
            LocalDateTime finDia = fechaConsulta.atTime(23, 59, 59);
            ZonedDateTime dateFrom = inicioDia.atZone(ZONE_UTC_MINUS_5);
            ZonedDateTime dateTo = finDia.atZone(ZONE_UTC_MINUS_5);
            String dateFromStr = dateFrom.format(DATETIME_FORMATTER);
            String dateToStr = dateTo.format(DATETIME_FORMATTER);
            
            log.info("📅 [CalculatedShiftService] Consultando viajes para la fecha: {} (desde {} hasta {})", 
                fechaConsulta, dateFromStr, dateToStr);
            
            // Obtener todos los conductores
            DriverListResponse driverListResponse = fleetDriverService.obtenerListaConductores(null);
            List<DriverListResponse.ContractorResponse> conductores = driverListResponse.getContractors();
            
            if (conductores == null || conductores.isEmpty()) {
                log.warn("⚠️ [CalculatedShiftService] No se encontraron conductores");
                return AllDriversOrdersResponse.builder()
                    .fecha(fechaConsulta.format(DATE_FORMATTER))
                    .conductores(new ArrayList<>())
                    .totalConductores(0)
                    .build();
            }
            
            log.info("📋 [CalculatedShiftService] Procesando {} conductores", conductores.size());
            
            // Para cada conductor, obtener sus viajes
            List<AllDriversOrdersResponse.DriverWithOrders> conductoresConViajes = new ArrayList<>();
            
            for (DriverListResponse.ContractorResponse conductor : conductores) {
                String driverId = conductor.getId();
                
                if (driverId == null || driverId.isEmpty()) {
                    log.warn("⚠️ [CalculatedShiftService] Conductor sin ID, saltando: {}", conductor.getFullName());
                    continue;
                }
                
                try {
                    // Obtener todos los viajes del conductor para esta fecha
                    DriverOrdersResponse viajesResponse = driverOrdersService.obtenerTodosLosViajes(
                        driverId, dateFromStr, dateToStr, null);
                    
                    List<OrderInfoResponse> viajes = viajesResponse.getOrders();
                    if (viajes == null) {
                        viajes = new ArrayList<>();
                    }
                    
                    AllDriversOrdersResponse.DriverWithOrders driverWithOrders = 
                        AllDriversOrdersResponse.DriverWithOrders.builder()
                            .driverId(driverId)
                            .fullName(conductor.getFullName())
                            .phone(conductor.getPhone())
                            .status(conductor.getStatus())
                            .viajes(viajes)
                            .totalViajes(viajes.size())
                            .build();
                    
                    conductoresConViajes.add(driverWithOrders);
                    
                    log.debug("✅ [CalculatedShiftService] Conductor {} tiene {} viajes", 
                        conductor.getFullName(), viajes.size());
                    
                } catch (Exception e) {
                    log.error("❌ [CalculatedShiftService] Error obteniendo viajes para conductor {} ({}): {}", 
                        conductor.getFullName(), driverId, e.getMessage());
                    
                    // Agregar el conductor sin viajes si hay error
                    AllDriversOrdersResponse.DriverWithOrders driverWithOrders = 
                        AllDriversOrdersResponse.DriverWithOrders.builder()
                            .driverId(driverId)
                            .fullName(conductor.getFullName())
                            .phone(conductor.getPhone())
                            .status(conductor.getStatus())
                            .viajes(new ArrayList<>())
                            .totalViajes(0)
                            .build();
                    
                    conductoresConViajes.add(driverWithOrders);
                }
            }
            
            log.info("✅ [CalculatedShiftService] Procesados {} conductores con sus viajes", conductoresConViajes.size());
            
            return AllDriversOrdersResponse.builder()
                .fecha(fechaConsulta.format(DATE_FORMATTER))
                .conductores(conductoresConViajes)
                .totalConductores(conductoresConViajes.size())
                .build();
            
        } catch (Exception e) {
            log.error("❌ [CalculatedShiftService] Error listando todos los conductores con viajes: {}", e.getMessage(), e);
            throw new RuntimeException("Error al listar todos los conductores con viajes: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId) {
        log.info("📅 [CalculatedShiftService] Obteniendo fechas con tipos de turno para driver_id: {}", driverId);
        
        try {
            // Obtener todos los CalculatedShift del driver ordenados por fecha
            List<CalculatedShift> shifts = calculatedShiftRepository.findByDriverIdOrderByFecha(driverId);
            
            if (shifts.isEmpty()) {
                log.info("ℹ️ [CalculatedShiftService] No se encontraron turnos para driver_id: {}", driverId);
                return FechasConTiposTurnoResponse.builder()
                        .driverId(driverId)
                        .fechas(new ArrayList<>())
                        .build();
            }
            
            // Agrupar por fecha
            Map<LocalDate, List<CalculatedShift>> shiftsPorFecha = shifts.stream()
                    .collect(Collectors.groupingBy(CalculatedShift::getFecha));
            
            // Construir la respuesta con fechas únicas y sus tipos de turno
            List<FechasConTiposTurnoResponse.FechaConTiposTurno> fechasConTipos = shiftsPorFecha.entrySet().stream()
                    .map(entry -> {
                        LocalDate fecha = entry.getKey();
                        List<CalculatedShift> shiftsDeFecha = entry.getValue();
                        
                        // Obtener los tipos de turno con sus IDs
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
}

