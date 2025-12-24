package com.yego.backend.service.yego_pro_ops.impl;

import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftManualRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.TurnoRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.AllDriversOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.CalculatedShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
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
            
            // 1. Consultar viajes del día anterior (fecha pasada como parámetro)
            ZonedDateTime inicioDia = fecha.atStartOfDay(ZONE_UTC_MINUS_5);
            ZonedDateTime finDia = fecha.atTime(23, 59, 59).atZone(ZONE_UTC_MINUS_5);
            
            DateTimeFormatter apiDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
            String dateFrom = inicioDia.format(apiDateFormatter);
            String dateTo = finDia.format(apiDateFormatter);
            
            DriverOrdersResponse respuesta = driverOrdersService.obtenerViajesCompletos(driverId, dateFrom, dateTo, null);
            List<OrderInfoResponse> viajes = respuesta.getOrders();
            
            if (viajes.isEmpty()) {
                log.debug("⚠️ [CalculatedShiftService] No se encontraron viajes para driver_id: {}, fecha: {}", driverId, fecha);
                return;
            }
            
            // 2. Encontrar hora de inicio (primer booked_at) y hora de fin (último ended_at)
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
                log.warn("⚠️ [CalculatedShiftService] No se pudo determinar hora de inicio para driver_id: {}, fecha: {}", driverId, fecha);
                return;
            }
            
            // 3. Determinar tipo de turno (diurno o nocturno) basado en la hora de inicio
            // Diurno: desde las 6:00 AM hasta las 6:00 PM (18:00)
            // Nocturno: el resto del tiempo
            CalculatedShift.TipoTurno tipoTurno;
            int horaInicioHora = horaInicio.getHour();
            if (horaInicioHora >= 6 && horaInicioHora < 18) {
                tipoTurno = CalculatedShift.TipoTurno.diurno;
            } else {
                tipoTurno = CalculatedShift.TipoTurno.nocturno;
            }
            
            log.debug("🕐 [CalculatedShiftService] Hora de inicio: {}, hora del día: {}, tipo de turno: {}", 
                horaInicio, horaInicioHora, tipoTurno);
            
            // 4. Calcular duración y estado
            Integer duracionMinutos = null;
            CalculatedShift.EstadoTurno estado;
            if (horaFin != null) {
                duracionMinutos = (int) Duration.between(horaInicio, horaFin).toMinutes();
                estado = CalculatedShift.EstadoTurno.finalizado;
            } else {
                estado = CalculatedShift.EstadoTurno.activo;
            }
            
            // 5. Buscar si ya existe un registro para este driver y fecha (día anterior)
            List<CalculatedShift> turnosExistentes = calculatedShiftRepository.findByDriverIdAndFecha(driverId, fecha);
            
            CalculatedShift turno;
            boolean esNuevo = turnosExistentes.isEmpty();
            
            if (esNuevo) {
                // Crear nuevo registro con la fecha del día anterior desde el inicio
                turno = new CalculatedShift();
                turno.setDriverId(driverId);
                turno.setFecha(fecha); // Fecha del día anterior (establecer PRIMERO)
                log.debug("💾 [CalculatedShiftService] Creando nuevo turno para driver_id: {}, fecha: {} (día anterior)", driverId, fecha);
            } else {
                // Actualizar el registro existente
                turno = turnosExistentes.get(0);
                // FORZAR la fecha del día anterior (puede que el registro existente tenga otra fecha)
                turno.setFecha(fecha); // Fecha del día anterior (forzar SIEMPRE)
                log.debug("🔄 [CalculatedShiftService] Actualizando turno existente ID: {} para driver_id: {}, fecha anterior: {}, fecha actual en BD: {}", 
                    turno.getId(), driverId, fecha, turnosExistentes.get(0).getFecha());
            }
            
            // 6. Establecer todos los demás campos (la fecha ya está establecida arriba)
            turno.setTipoTurno(tipoTurno);
            turno.setHoraInicio(horaInicio);
            turno.setHoraFin(horaFin);
            turno.setEstado(estado);
            turno.setDuracionMinutos(duracionMinutos);
            
            // 7. Verificar una vez más que la fecha sea la correcta ANTES de guardar
            if (!turno.getFecha().equals(fecha)) {
                log.error("❌ [CalculatedShiftService] ERROR: La fecha del turno ({}) no coincide con la esperada ({}). Corrigiendo...", 
                    turno.getFecha(), fecha);
                turno.setFecha(fecha);
            }
            
            // 8. Guardar y hacer flush para asegurar que se persista inmediatamente
            CalculatedShift saved = calculatedShiftRepository.save(turno);
            calculatedShiftRepository.flush(); // Forzar el flush para asegurar que se guarde inmediatamente
            
            // Verificar que se guardó correctamente leyendo de la BD
            CalculatedShift verificado = calculatedShiftRepository.findById(saved.getId()).orElse(null);
            if (verificado != null) {
                log.info("✅ [CalculatedShiftService] Turno guardado y verificado - ID: {}, driver_id: {}, fecha: {}, tipo: {}, inicio: {}, fin: {}, duracion: {} min", 
                    verificado.getId(), driverId, verificado.getFecha(), verificado.getTipoTurno(), verificado.getHoraInicio(), verificado.getHoraFin(), verificado.getDuracionMinutos());
            } else {
                log.error("❌ [CalculatedShiftService] ERROR: El turno no se encontró en la BD después de guardar. ID: {}", saved.getId());
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
                    
                    // Log cada 5 conductores procesados para seguimiento
                    if (totalProcesados % 5 == 0) {
                        log.info("📈 [CalculatedShiftService] Progreso: {}/{} conductores procesados y guardados", totalProcesados, totalConductores);
                    }
                } catch (Exception e) {
                    log.error("❌ [CalculatedShiftService] Error procesando horas de turno para driver_id: {}, fecha: {}: {}", 
                        contractor.getId(), fechaAnterior, e.getMessage(), e);
                    totalErrores++;
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
            
            // Validar tipo de turno
            CalculatedShift.TipoTurno tipoTurno;
            try {
                tipoTurno = CalculatedShift.TipoTurno.valueOf(request.getTipoTurno().toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de turno inválido: " + request.getTipoTurno() + ". Debe ser 'diurno' o 'nocturno'");
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
            
            // Buscar si ya existe un turno para este conductor y fecha
            List<CalculatedShift> turnosExistentes = calculatedShiftRepository.findByDriverIdAndFecha(request.getDriverId(), fecha);
            
            CalculatedShift turno;
            if (!turnosExistentes.isEmpty()) {
                // Actualizar el turno existente
                turno = turnosExistentes.get(0);
                log.info("🔄 [CalculatedShiftService] Actualizando turno existente ID: {} para driverId: {}, fecha: {}", 
                    turno.getId(), request.getDriverId(), fecha);
            } else {
                // Crear nuevo turno
                turno = new CalculatedShift();
                turno.setDriverId(request.getDriverId());
                log.info("💾 [CalculatedShiftService] Creando nuevo turno manual para driverId: {}, fecha: {}", 
                    request.getDriverId(), fecha);
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
}

