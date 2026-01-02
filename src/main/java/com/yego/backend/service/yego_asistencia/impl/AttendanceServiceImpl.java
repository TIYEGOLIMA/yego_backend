package com.yego.backend.service.yego_asistencia.impl;

import com.yego.backend.entity.yego_asistencia.entities.AttendanceRecord;
import com.yego.backend.entity.yego_asistencia.entities.AttendanceType;
import com.yego.backend.repository.yego_asistencia.AttendanceRepository;
import com.yego.backend.service.yego_asistencia.AttendanceService;
import com.yego.backend.service.yego_asistencia.MessageService;
import com.yego.backend.handler.yego_asistencia.AttendanceNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final MessageService messageService;
    private final AttendanceNotificationHandler attendanceNotificationHandler;

    // ===== MÉTODOS PRINCIPALES DE MARCACIÓN =====

    @Override
    public Map<String, Object> processAttendanceMarkingWithIpValidation(Long userId, Map<String, Object> attendanceData, String clientIp) {
        log.info("🎯 [AttendanceService] Procesando marcación con validación de IP para usuario: {}", userId);
        
        // Validar IP autorizada
        if (clientIp == null) {
            log.warn("⚠️ [AttendanceService] No se pudo determinar la IP del cliente");
            return Map.of(
                "success", false,
                "message", "No se pudo determinar la IP del cliente"
            );
        }
        
        boolean ipAutorizada = isAuthorizedIp(clientIp);
        if (!ipAutorizada) {
            log.warn("🚫 [AttendanceService] Intento de marcación desde IP no autorizada: {} - Usuario: {}", clientIp, userId);
            return Map.of(
                "success", false,
                "message", "Acceso denegado: IP no autorizada para marcación de asistencia",
                "ip", clientIp
            );
        }
        
        log.info("✅ [AttendanceService] Marcación autorizada desde IP: {} - Usuario: {}", clientIp, userId);
        
        // Procesar marcación normalmente
        return processAttendanceMarking(userId, attendanceData);
    }

    @Override
    public String validateAttendanceSequence(Long userId, String attendanceType) {
        log.info("🔍 [AttendanceService] Validando secuencia de marcación para usuario: {}", userId);
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        log.info("🔍 [AttendanceService] Buscando último registro para usuario: {} en fecha: {}", userId, today);
        
        Optional<AttendanceRecord> lastRecord;
        try {
            List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtDesc(userId, today);
            lastRecord = records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
            log.info("🔍 [AttendanceService] Consulta completada. Registros encontrados: {}, Último registro: {}", records.size(), lastRecord.isPresent());
            if (lastRecord.isPresent()) {
                log.info("🔍 [AttendanceService] Último tipo de marcación: {}", lastRecord.get().getAttendanceType());
            }
        } catch (Exception e) {
            log.error("❌ [AttendanceService] Error en consulta de último registro: {}", e.getMessage(), e);
            return "Error al validar secuencia de marcación";
        }

        log.info("🔍 [AttendanceService] Convirtiendo tipo de asistencia: {}", attendanceType);
        AttendanceType type = AttendanceType.fromValue(attendanceType);
        log.info("🔍 [AttendanceService] Tipo convertido: {}", type);

        // Validación crítica: No permitir marcaciones después de EXIT (salida) en el mismo día
        if (lastRecord.isPresent() && lastRecord.get().getAttendanceType() == AttendanceType.EXIT) {
            log.warn("⚠️ [AttendanceService] Error: No se pueden realizar marcaciones después de marcar salida en el mismo día. Jornada laboral finalizada para hoy.");
            return "No se pueden realizar marcaciones después de marcar salida. Tu jornada laboral de hoy ya ha finalizado. Podrás marcar entrada mañana.";
        }

        if (type == AttendanceType.ENTRY && lastRecord.isPresent() && lastRecord.get().getAttendanceType() != AttendanceType.EXIT) {
            log.warn("⚠️ [AttendanceService] Error: Debes completar la jornada anterior antes de marcar entrada");
            return "Debes completar la jornada anterior antes de marcar entrada";
        }

        if (type == AttendanceType.EXIT_BREAK && (lastRecord.isEmpty() || lastRecord.get().getAttendanceType() != AttendanceType.ENTRY)) {
            log.warn("⚠️ [AttendanceService] Error: Debes marcar entrada primero");
            return "Debes marcar entrada primero";
        }

        if (type == AttendanceType.RETURN_BREAK && (lastRecord.isEmpty() || lastRecord.get().getAttendanceType() != AttendanceType.EXIT_BREAK)) {
            log.warn("⚠️ [AttendanceService] Error: Debes marcar salida a refrigerio primero. Último registro: {}", 
                lastRecord.isPresent() ? lastRecord.get().getAttendanceType() : "NINGUNO");
            return "Debes marcar salida a refrigerio primero";
        }

        if (type == AttendanceType.EXIT && (lastRecord.isEmpty() || lastRecord.get().getAttendanceType() != AttendanceType.RETURN_BREAK)) {
            log.warn("⚠️ [AttendanceService] Error: Debes marcar regreso de refrigerio primero");
            return "Debes marcar regreso de refrigerio primero";
        }

        log.info("✅ [AttendanceService] Secuencia de marcación válida");
        return null; // Secuencia válida
    }

    @Override
    public AttendanceRecord createAttendanceRecord(Long userId, String attendanceType,
                                                 String computerName, String windowsUsername,
                                                 String localIp, String machineId) {
        log.info("📝 [AttendanceService] Creando registro de asistencia para usuario: {}", userId);
        
        AttendanceRecord record = new AttendanceRecord(userId, AttendanceType.fromValue(attendanceType));

        // Configurar información adicional
        record.setComputerName(computerName);
        record.setWindowsUsername(windowsUsername);
        record.setLocalIp(localIp);
        record.setMachineId(machineId);
        record.setPublicIp(getClientIp());
        record.setUserAgent(getUserAgent());
        record.setBrowserName(getBrowserName());
        record.setBrowserVersion(getBrowserVersion());
        record.setOperatingSystem(getOperatingSystem());
        record.setTimezone("America/Lima");
        record.setLanguage("es");
        record.setIsManual(false);

        AttendanceRecord savedRecord = attendanceRepository.save(record);
        log.info("✅ [AttendanceService] Registro creado con ID: {}", savedRecord.getId());
        return savedRecord;
    }

    @Override
    public Map<String, Object> processAttendanceMarking(Long userId, Map<String, Object> attendanceData) {
        log.info("🎯 [AttendanceService] Procesando marcación para usuario: {}", userId);

        // Extraer datos del request
        String tipo = (String) attendanceData.get("tipo");
        String computerName = (String) attendanceData.get("computer_name");
        String windowsUsername = (String) attendanceData.get("windows_username");
        String localIp = (String) attendanceData.get("local_ip");
        String machineId = (String) attendanceData.get("machine_id");

        // Mapear tipos del frontend a la base de datos
        Map<String, String> tipoMap = Map.of(
            "entrada", "entry",
            "salida_refrigerio", "exit_break",
            "regreso_refrigerio", "return_break",
            "salida", "exit"
        );

        String attendanceType = tipoMap.get(tipo);
        if (attendanceType == null) {
            log.warn("⚠️ [AttendanceService] Tipo de marcación inválido: {}", tipo);
            return Map.of(
                "success", false,
                "message", "Tipo de marcación inválido"
            );
        }

        // Validar secuencia de marcaciones
        String validationError = validateAttendanceSequence(userId, attendanceType);
        if (validationError != null) {
            log.warn("⚠️ [AttendanceService] Error de validación: {}", validationError);
            return Map.of(
                "success", false,
                "message", validationError
            );
        }

        // Crear marcación
        AttendanceRecord record = createAttendanceRecord(userId, attendanceType, computerName,
                                                       windowsUsername, localIp, machineId);

        // Obtener mensaje motivacional
        String mensajeMotivacional = messageService.getRandomMessage(tipo);

        // Enviar actualización de registros de hoy por WebSocket
        try {
            // Obtener registros actualizados del día
            List<Map<String, Object>> registrosHoy = getTodayAttendances(userId);
            
            // Enviar solo la lista actualizada de registros de hoy
            attendanceNotificationHandler.enviarActualizacionRegistrosHoy(userId, registrosHoy);
            
            log.info("📤 [AttendanceService] Lista actualizada de registros enviada para usuario: {}", userId);
        } catch (Exception e) {
            log.warn("⚠️ [AttendanceService] Error enviando actualización de registros: {}", e.getMessage());
        }

        log.info("✅ [AttendanceService] Marcación creada exitosamente con ID: {}", record.getId());

        return Map.of(
            "success", true,
            "message", "Marcación registrada exitosamente",
            "recordId", record.getId(),
            "attendanceType", attendanceType,
            "timestamp", record.getRecordedAt(),
            "mensajeMotivacional", mensajeMotivacional
        );
    }

    // ===== MÉTODOS DE CONSULTA BÁSICA =====

    @Override
    public List<Map<String, Object>> getTodayAttendances(Long userId) {
        log.info("📅 [AttendanceService] Obteniendo marcaciones del día para usuario: {}", userId);
        
        // Debug: Mostrar diferentes zonas horarias
        LocalDate todayServer = LocalDate.now();
        LocalDate todayPeru = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        log.info("📅 [AttendanceService] Fecha servidor: {}, Fecha Perú: {}", todayServer, todayPeru);
        
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = todayPeru;
        log.info("📅 [AttendanceService] Fecha de consulta final: {}", today);
        
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtAsc(userId, today);
        log.info("📅 [AttendanceService] Registros encontrados en BD: {}", records.size());
        
        // Debug: Mostrar fechas de los registros encontrados
        for (AttendanceRecord record : records) {
            log.info("📅 [AttendanceService] Registro encontrado - ID: {}, Fecha: {}, Tipo: {}", 
                record.getId(), record.getRecordedDate(), record.getAttendanceType());
        }

        Map<String, String> tipoMap = Map.of(
            "ENTRY", "entrada",
            "EXIT_BREAK", "salida_refrigerio",
            "RETURN_BREAK", "regreso_refrigerio",
            "EXIT", "salida"
        );

        List<Map<String, Object>> marcaciones = new ArrayList<>();
        for (AttendanceRecord record : records) {
            log.info("📅 [AttendanceService] Procesando registro ID: {}, Tipo: {}, Fecha: {}", 
                record.getId(), record.getAttendanceType(), record.getRecordedDate());
            
            Map<String, Object> marcacion = new HashMap<>();
            marcacion.put("id", record.getId());
            marcacion.put("empleadoId", userId);
            marcacion.put("tipo", tipoMap.get(record.getAttendanceType().name()));
            marcacion.put("fecha", record.getRecordedDate().toString());
            marcacion.put("hora", record.getRecordedTime().toString());
            marcacion.put("timestamp", record.getRecordedAt().toString());
            marcacion.put("ip", record.getPublicIp());
            marcaciones.add(marcacion);
        }

        log.info("✅ [AttendanceService] Encontradas {} marcaciones para el día", marcaciones.size());
        return marcaciones;
    }

    @Override
    public Map<String, Object> getEmployeeStatistics(Long userId) {
        log.info("📊 [AttendanceService] Obteniendo estadísticas para usuario: {}", userId);
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtAsc(userId, today);

        Map<String, String> tipoMap = Map.of(
            "ENTRY", "entrada",
            "EXIT_BREAK", "salida_refrigerio",
            "RETURN_BREAK", "regreso_refrigerio",
            "EXIT", "salida"
        );

        List<Map<String, Object>> marcaciones = new ArrayList<>();
        for (AttendanceRecord record : records) {
            Map<String, Object> marcacion = new HashMap<>();
            marcacion.put("id", record.getId());
            marcacion.put("empleadoId", userId);
            marcacion.put("tipo", tipoMap.get(record.getAttendanceType().name()));
            marcacion.put("fecha", record.getRecordedDate().toString());
            marcacion.put("hora", record.getRecordedTime().toString());
            marcacion.put("timestamp", record.getRecordedAt().toString());
            marcaciones.add(marcacion);
        }

        // Calcular tiempo trabajado
        double tiempoTrabajado = 0.0;
        if (records.size() >= 2) {
            Optional<AttendanceRecord> entrada = records.stream()
                .filter(r -> r.getAttendanceType() == AttendanceType.ENTRY)
                .findFirst();
            Optional<AttendanceRecord> salida = records.stream()
                .filter(r -> r.getAttendanceType() == AttendanceType.EXIT)
                .findFirst();

            if (entrada.isPresent() && salida.isPresent()) {
                long minutes = ChronoUnit.MINUTES.between(entrada.get().getRecordedAt(), salida.get().getRecordedAt());
                tiempoTrabajado = minutes / 60.0;
            }
        }

        Map<String, Object> ultimaMarcacion = marcaciones.isEmpty() ? null : marcaciones.get(marcaciones.size() - 1);

        // Calcular estados de marcación
        boolean puedeMarcarEntrada = canMarkEntry(userId);
        boolean puedeMarcarSalida = canMarkExit(userId);
        boolean estaTrabajando = isUserCurrentlyWorking(userId);
        boolean estaEnRefrigerio = isUserOnBreak(userId);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("tiempoTrabajado", Math.round(tiempoTrabajado * 10.0) / 10.0);
        estadisticas.put("totalMarcaciones", records.size());
        estadisticas.put("ultimaMarcacion", ultimaMarcacion);
        estadisticas.put("marcacionesHoy", marcaciones);
        
        // Estados de marcación - Lógica corregida
        estadisticas.put("puedeMarcarEntrada", puedeMarcarEntrada);
        estadisticas.put("puedeMarcarSalida", puedeMarcarSalida);
        
        // Solo puede marcar salida a refrigerio si está trabajando Y no está en refrigerio Y no ha regresado del refrigerio
        String ultimoTipo = marcaciones.isEmpty() ? "NINGUNO" : (String) marcaciones.get(marcaciones.size() - 1).get("tipo");
        boolean puedeMarcarSalidaRefrigerio = estaTrabajando && !estaEnRefrigerio && 
            (marcaciones.isEmpty() || !ultimoTipo.equals("regreso_refrigerio"));
        
        log.info("🔍 [AttendanceService] Lógica salida refrigerio - estaTrabajando: {}, estaEnRefrigerio: {}, ultimoTipo: {}, puedeMarcarSalidaRefrigerio: {}", 
            estaTrabajando, estaEnRefrigerio, ultimoTipo, puedeMarcarSalidaRefrigerio);
        
        estadisticas.put("puedeMarcarSalidaRefrigerio", puedeMarcarSalidaRefrigerio);
        estadisticas.put("puedeMarcarRegresoRefrigerio", estaEnRefrigerio);
        estadisticas.put("estaTrabajando", estaTrabajando);
        estadisticas.put("estaEnRefrigerio", estaEnRefrigerio);

        log.info("✅ [AttendanceService] Estadísticas generadas para usuario: {} - Estados: entrada={}, salida={}, trabajando={}, refrigerio={}", 
                userId, puedeMarcarEntrada, puedeMarcarSalida, estaTrabajando, estaEnRefrigerio);
        return estadisticas;
    }

    @Override
    public Optional<AttendanceRecord> getLastAttendanceRecord(Long userId) {
        log.info("🔍 [AttendanceService] Obteniendo última marcación del usuario: {}", userId);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtDesc(userId, today);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    @Override
    public List<AttendanceRecord> getAttendanceRecordsByUserAndDate(Long userId, LocalDate date) {
        log.info("📅 [AttendanceService] Obteniendo registros de usuario {} para fecha: {}", userId, date);
        return attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtAsc(userId, date);
    }

    @Override
    public List<AttendanceRecord> getAttendanceRecordsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("📅 [AttendanceService] Obteniendo registros de usuario {} entre {} y {}", userId, startDate, endDate);
        return attendanceRepository.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    // ===== MÉTODOS DE VALIDACIÓN =====

    @Override
    public boolean isAuthorizedIp(String ip) {
        log.info("🌐 [AttendanceService] Verificando IP autorizada: {}", ip);

        // Normalizar IP antes de validar
        String normalizedIp = normalizeIp(ip);
        log.info("🔄 [AttendanceService] IP normalizada: {} -> {}", ip, normalizedIp);

        // Lista de IPs autorizadas para marcación de asistencia
        List<String> authorizedIps = Arrays.asList(
            "161.132.204.202",
            "190.81.48.242",
            "38.253.176.89", 
            "10.10.10.12",
            "127.0.0.1"         // IPv4 localhost (IPv6 se convierte automáticamente)
        );

        // Verificar si la IP está en la lista autorizada
        boolean isAuthorized = authorizedIps.contains(normalizedIp);

        log.info("✅ [AttendanceService] IP {} autorizada: {}", normalizedIp, isAuthorized);
        return isAuthorized;
    }
    
    /**
     * Normalizar IP para priorizar IPv4 sobre IPv6
     */
    private String normalizeIp(String ip) {
        if (ip == null || "unknown".equalsIgnoreCase(ip)) {
            return "unknown";
        }
        
        // Si es IPv6 localhost, convertir a IPv4 localhost
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            log.info("🔄 [AttendanceService] Convirtiendo IPv6 localhost a IPv4: {} -> 127.0.0.1", ip);
            return "127.0.0.1";
        }
        
        return ip;
    }

    @Override
    public boolean canMarkEntry(Long userId) {
        log.info("🔍 [AttendanceService] Verificando si usuario {} puede marcar entrada", userId);
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtDesc(userId, today);
        Optional<AttendanceRecord> lastRecord = records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        
        // Puede marcar entrada si no tiene marcaciones o si la última es salida
        boolean canMark = lastRecord.isEmpty() || lastRecord.get().getAttendanceType() == AttendanceType.EXIT;
        log.info("✅ [AttendanceService] Usuario {} puede marcar entrada: {}", userId, canMark);
        return canMark;
    }

    @Override
    public boolean canMarkExit(Long userId) {
        log.info("🔍 [AttendanceService] Verificando si usuario {} puede marcar salida", userId);
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtDesc(userId, today);
        Optional<AttendanceRecord> lastRecord = records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        
        // Puede marcar salida si la última marcación es regreso de refrigerio
        boolean canMark = lastRecord.isPresent() && lastRecord.get().getAttendanceType() == AttendanceType.RETURN_BREAK;
        log.info("✅ [AttendanceService] Usuario {} puede marcar salida: {}", userId, canMark);
        return canMark;
    }

    @Override
    public boolean isUserCurrentlyWorking(Long userId) {
        log.info("🔍 [AttendanceService] Verificando si usuario {} está trabajando actualmente", userId);
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtDesc(userId, today);
        Optional<AttendanceRecord> lastRecord = records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        
        if (lastRecord.isEmpty()) return false;
        
        AttendanceType lastType = lastRecord.get().getAttendanceType();
        boolean isWorking = lastType == AttendanceType.ENTRY || lastType == AttendanceType.RETURN_BREAK;
        
        log.info("✅ [AttendanceService] Usuario {} está trabajando: {}", userId, isWorking);
        return isWorking;
    }

    @Override
    public boolean isUserOnBreak(Long userId) {
        // Usar zona horaria de Perú (America/Lima) como en otras partes del sistema
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtDesc(userId, today);
        Optional<AttendanceRecord> lastRecord = records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
        boolean onBreak = lastRecord.isPresent() && lastRecord.get().getAttendanceType() == AttendanceType.EXIT_BREAK;
        log.info("🔍 [AttendanceService] Usuario {} está en refrigerio: {} (último tipo: {})", 
            userId, onBreak, lastRecord.isPresent() ? lastRecord.get().getAttendanceType() : "NINGUNO");
        return onBreak;
    }

    // ===== MÉTODOS DE ESTADÍSTICAS =====

    @Override
    public List<Map<String, Object>> getAttendanceRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("📅 [AttendanceService] Obteniendo marcaciones por rango para usuario: {} desde {} hasta {}", userId, startDate, endDate);
        
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        log.info("📅 [AttendanceService] Registros encontrados en rango: {}", records.size());

        Map<String, String> tipoMap = Map.of(
            "ENTRY", "entrada",
            "EXIT_BREAK", "salida_refrigerio",
            "RETURN_BREAK", "regreso_refrigerio",
            "EXIT", "salida"
        );

        List<Map<String, Object>> marcaciones = new ArrayList<>();
        for (AttendanceRecord record : records) {
            log.info("📅 [AttendanceService] Procesando registro ID: {}, Tipo: {}, Fecha: {}", 
                record.getId(), record.getAttendanceType(), record.getRecordedDate());
            
            Map<String, Object> marcacion = new HashMap<>();
            marcacion.put("id", record.getId());
            marcacion.put("empleadoId", userId);
            marcacion.put("tipo", tipoMap.get(record.getAttendanceType().name()));
            marcacion.put("fecha", record.getRecordedDate().toString());
            marcacion.put("hora", record.getRecordedTime().toString());
            marcacion.put("timestamp", record.getRecordedAt().toString());
            marcacion.put("ip", record.getPublicIp());
            marcacion.put("dispositivo", record.getBrowserName() + "/" + record.getOperatingSystem());
            marcacion.put("observaciones", null); // Campo para futuras observaciones
            marcacion.put("fechaCreacion", record.getCreatedAt().toString());
            marcaciones.add(marcacion);
        }

        log.info("✅ [AttendanceService] Encontradas {} marcaciones en el rango especificado", marcaciones.size());
        return marcaciones;
    }

    @Override
    public Map<String, Object> getWorkedTimeToday(Long userId) {
        log.info("⏰ [AttendanceService] Calculando tiempo trabajado del día para usuario: {}", userId);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndRecordedDateOrderByRecordedAtAsc(userId, today);
        
        double tiempoTrabajado = 0.0;
        if (records.size() >= 2) {
            Optional<AttendanceRecord> entrada = records.stream()
                .filter(r -> r.getAttendanceType() == AttendanceType.ENTRY)
                .findFirst();
            Optional<AttendanceRecord> salida = records.stream()
                .filter(r -> r.getAttendanceType() == AttendanceType.EXIT)
                .findFirst();

            if (entrada.isPresent() && salida.isPresent()) {
                long minutes = ChronoUnit.MINUTES.between(entrada.get().getRecordedAt(), salida.get().getRecordedAt());
                tiempoTrabajado = minutes / 60.0;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tiempoTrabajado", Math.round(tiempoTrabajado * 10.0) / 10.0);
        result.put("totalMarcaciones", records.size());
        result.put("fecha", today.toString());
        
        log.info("✅ [AttendanceService] Tiempo trabajado calculado: {} horas", tiempoTrabajado);
        return result;
    }

    @Override
    public Map<String, Object> getWorkedTimeInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("⏰ [AttendanceService] Calculando tiempo trabajado en rango {} - {} para usuario: {}", startDate, endDate, userId);
        List<AttendanceRecord> records = attendanceRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        double tiempoTotal = 0.0;
        int diasTrabajados = 0;
        
        // Agrupar por fecha y calcular tiempo por día
        Map<LocalDate, List<AttendanceRecord>> recordsByDate = records.stream()
            .collect(Collectors.groupingBy(AttendanceRecord::getRecordedDate));
        
        for (Map.Entry<LocalDate, List<AttendanceRecord>> entry : recordsByDate.entrySet()) {
            List<AttendanceRecord> dayRecords = entry.getValue();
            if (dayRecords.size() >= 2) {
                Optional<AttendanceRecord> entrada = dayRecords.stream()
                    .filter(r -> r.getAttendanceType() == AttendanceType.ENTRY)
                    .findFirst();
                Optional<AttendanceRecord> salida = dayRecords.stream()
                    .filter(r -> r.getAttendanceType() == AttendanceType.EXIT)
                    .findFirst();

                if (entrada.isPresent() && salida.isPresent()) {
                    long minutes = ChronoUnit.MINUTES.between(entrada.get().getRecordedAt(), salida.get().getRecordedAt());
                    tiempoTotal += minutes / 60.0;
                    diasTrabajados++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tiempoTotal", Math.round(tiempoTotal * 10.0) / 10.0);
        result.put("diasTrabajados", diasTrabajados);
        result.put("promedioDiario", diasTrabajados > 0 ? Math.round((tiempoTotal / diasTrabajados) * 10.0) / 10.0 : 0.0);
        result.put("fechaInicio", startDate.toString());
        result.put("fechaFin", endDate.toString());
        
        log.info("✅ [AttendanceService] Tiempo total calculado: {} horas en {} días", tiempoTotal, diasTrabajados);
        return result;
    }

    // ===== MÉTODOS DE ADMINISTRACIÓN =====

    @Override
    public Page<AttendanceRecord> getAllAttendanceRecords(Pageable pageable) {
        log.info("📄 [AttendanceService] Obteniendo todos los registros de asistencia paginados");
        return attendanceRepository.findAll(pageable);
    }

    @Override
    public Optional<AttendanceRecord> getAttendanceRecordById(Long id) {
        log.info("🔍 [AttendanceService] Obteniendo registro de asistencia con ID: {}", id);
        return attendanceRepository.findById(id);
    }

    @Override
    public AttendanceRecord updateAttendanceRecord(Long id, AttendanceRecord attendanceRecord) {
        log.info("✏️ [AttendanceService] Actualizando registro de asistencia con ID: {}", id);
        if (attendanceRepository.existsById(id)) {
            attendanceRecord.setId(id);
            return attendanceRepository.save(attendanceRecord);
        }
        return null;
    }

    @Override
    public void deleteAttendanceRecord(Long id) {
        log.info("🗑️ [AttendanceService] Eliminando registro de asistencia con ID: {}", id);
        attendanceRepository.deleteById(id);
        log.info("✅ [AttendanceService] Registro eliminado exitosamente");
    }

    @Override
    public byte[] exportAttendanceRecords(LocalDate startDate, LocalDate endDate, String format) {
        log.info("📊 [AttendanceService] Exportando registros entre {} y {} en formato {}", startDate, endDate, format);
        // Implementar lógica de exportación
        return "Exportación completada".getBytes();
    }

    // ===== MÉTODOS AUXILIARES =====

    private String getClientIp() {
        return "127.0.0.1";
    }

    private String getUserAgent() {
        // Implementar lógica para obtener User-Agent
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    }

    private String getBrowserName() {
        return "Chrome";
    }

    private String getBrowserVersion() {
        return "120.0.0.0";
    }

    private String getOperatingSystem() {
        return "Windows";
    }
    
    /**
     * Obtener marcaciones por rol
     */
    @Override
    public List<Map<String, Object>> getAttendanceRecordsByRole(Long userId, String userRole, LocalDate fecha) {
        log.info("👥 [AttendanceService] Obteniendo marcaciones por rol para usuario: {} (rol: {}) en fecha: {}", userId, userRole, fecha);
        
        List<Object[]> recordsWithNames;
        
        // Para el historial de asistencia, siempre devolver solo las marcaciones del usuario específico
        log.info("👤 [AttendanceService] Obteniendo marcaciones del usuario específico: {}", userId);
        recordsWithNames = attendanceRepository.findByUserIdAndDateWithUserNames(userId, fecha);
        
        log.info("📊 [AttendanceService] Registros encontrados por rol: {}", recordsWithNames.size());
        
        // Mapear a formato de respuesta
        Map<String, String> tipoMap = Map.of(
            "ENTRY", "entrada",
            "EXIT_BREAK", "salida_refrigerio", 
            "RETURN_BREAK", "regreso_refrigerio",
            "EXIT", "salida"
        );
        
        List<Map<String, Object>> marcaciones = new ArrayList<>();
        for (Object[] recordData : recordsWithNames) {
            log.info("🔍 [AttendanceService] Procesando registro con {} columnas", recordData.length);
            
            try {
                Long id = ((Number) recordData[0]).longValue();
                Long empleadoId = ((Number) recordData[1]).longValue();
                String attendanceType = (String) recordData[2];
            
            // Convertir fechas de java.sql a java.time
            // Manejar diferentes órdenes de columnas entre desarrollo y producción
            LocalDate recordedDate;
            LocalTime recordedTime;
            LocalDateTime recordedAt;
            
            // Detectar el orden basado en los tipos de datos
            if (recordData[3] instanceof java.sql.Timestamp) {
                // Producción: recordData[3] = TIMESTAMP, recordData[4] = DATE, recordData[5] = TIME
                recordedAt = ((java.sql.Timestamp) recordData[3]).toLocalDateTime();
                recordedDate = ((java.sql.Date) recordData[4]).toLocalDate();
                recordedTime = ((java.sql.Time) recordData[5]).toLocalTime();
            } else {
                // Desarrollo: recordData[3] = DATE, recordData[4] = TIME, recordData[5] = TIMESTAMP
                recordedDate = ((java.sql.Date) recordData[3]).toLocalDate();
                recordedTime = ((java.sql.Time) recordData[4]).toLocalTime();
                recordedAt = ((java.sql.Timestamp) recordData[5]).toLocalDateTime();
            }
            
            // Manejar createdAt que puede ser null
            LocalDateTime createdAt = null;
            if (recordData[20] != null) {
                createdAt = ((java.sql.Timestamp) recordData[20]).toLocalDateTime();
            } else {
                createdAt = recordedAt; // Usar recordedAt como fallback
            }
            
            // Mapear campos de texto con verificación de tipos
            String browserName = "Unknown";
            if (recordData.length > 12 && recordData[12] != null && recordData[12] instanceof String) {
                browserName = (String) recordData[12];
            }
            
            String operatingSystem = "Unknown";
            if (recordData.length > 14 && recordData[14] != null && recordData[14] instanceof String) {
                operatingSystem = (String) recordData[14];
            }
            
            String fullName = "Usuario " + empleadoId;
            if (recordData.length > 22 && recordData[22] != null && recordData[22] instanceof String) {
                fullName = (String) recordData[22];
            }
        
            Map<String, Object> marcacion = new HashMap<>();
            marcacion.put("id", id);
            marcacion.put("empleadoId", empleadoId);
            marcacion.put("nombreCompleto", fullName);
            marcacion.put("tipo", tipoMap.get(attendanceType));
            marcacion.put("fecha", recordedDate.toString());
            marcacion.put("hora", recordedTime.toString());
            marcacion.put("timestamp", recordedAt.toString());
            marcacion.put("dispositivo", browserName + "/" + operatingSystem);
            marcacion.put("fechaCreacion", createdAt.toString());
            marcaciones.add(marcacion);
            } catch (Exception e) {
                log.error("❌ [AttendanceService] Error al procesar registro: {}", e.getMessage(), e);
                log.error("❌ [AttendanceService] Datos del registro: {}", java.util.Arrays.toString(recordData));
                // Continuar con el siguiente registro en lugar de fallar completamente
                continue;
            }
        }
        
        log.info("✅ [AttendanceService] Encontradas {} marcaciones por rol", marcaciones.size());
        return marcaciones;
    }
    
    /**
     * Obtener usuarios por rol
     */
    @Override
    public List<Map<String, Object>> getUsersByRole(String userRole) {
        log.info("👥 [AttendanceService] Obteniendo usuarios por rol: {}", userRole);
        
        List<Object[]> usersData;
        
        // Filtrar según el rol del usuario
        if ("ADMIN".equalsIgnoreCase(userRole) || "SUPERADMIN".equalsIgnoreCase(userRole)) {
            // Admin y SuperAdmin ven todos los usuarios
            log.info("🔓 [AttendanceService] Usuario {} - obteniendo todos los usuarios", userRole);
            usersData = attendanceRepository.findAllUsers();
        } else if ("SUPERVISOR".equalsIgnoreCase(userRole)) {
            // SUPERVISOR solo puede ver SAC, SUPERVISOR y OPERADOR
            log.info("👁️ [AttendanceService] Usuario SUPERVISOR - obteniendo solo SAC, SUPERVISOR y OPERADOR");
            List<Object[]> allUsers = attendanceRepository.findAllUsers();
            usersData = allUsers.stream()
                    .filter(userData -> {
                        String role = (String) userData[3]; // El rol está en la posición 3
                        return "SAC".equalsIgnoreCase(role) || 
                               "SUPERVISOR".equalsIgnoreCase(role) || 
                               "OPERADOR".equalsIgnoreCase(role);
                    })
                    .collect(Collectors.toList());
        } else {
            // Otros roles no pueden ver usuarios
            log.info("👤 [AttendanceService] Usuario {} - sin permisos para ver usuarios", userRole);
            return new ArrayList<>();
        }
        
        log.info("📊 [AttendanceService] Usuarios encontrados: {}", usersData.size());
        
        // Mapear a formato de respuesta
        List<Map<String, Object>> usuarios = new ArrayList<>();
        for (Object[] userData : usersData) {
            Long id = ((Number) userData[0]).longValue();
            String name = (String) userData[1];
            String lastName = (String) userData[2];
            String role = (String) userData[3];
            String email = (String) userData[4];
            
            String fullName = (name != null && lastName != null) ? name + " " + lastName : "Usuario " + id;
            
            Map<String, Object> usuario = new HashMap<>();
            usuario.put("id", id);
            usuario.put("nombreCompleto", fullName);
            usuario.put("rol", role);
            usuario.put("email", email != null ? email : "");
            usuarios.add(usuario);
        }
        
        log.info("✅ [AttendanceService] Encontrados {} usuarios por rol", usuarios.size());
        return usuarios;
    }
    
    @Override
    public byte[] exportarMarcacionesPorRangoDeFechasYRol(String fechaInicio, String fechaFin, String rol, String rolUsuarioGenerador) {
        log.info("📊 [AttendanceService] Exportando marcaciones para rango de fechas: {} - {} y rol: {} - Generado por: {}", 
            fechaInicio, fechaFin, rol, rolUsuarioGenerador);
        
        // Parsear fechas (las validaciones ya se hicieron en el controller)
        LocalDate fechaConsultaInicio = LocalDate.parse(fechaInicio);
        LocalDate fechaConsultaFin = LocalDate.parse(fechaFin);
        
        try {
            // Obtener marcaciones por rango de fechas y rol
            // Si el rol es "TODOS", obtener todas las marcaciones sin filtrar por rol
            List<Object[]> recordsWithNames;
            if (rol != null && rol.equalsIgnoreCase("TODOS")) {
                log.info("📊 [AttendanceService] Rol es TODOS - Obteniendo marcaciones de todos los roles");
                recordsWithNames = attendanceRepository.findByDateRangeAllRoles(fechaConsultaInicio, fechaConsultaFin);
            } else {
                recordsWithNames = attendanceRepository.findByDateRangeAndRole(fechaConsultaInicio, fechaConsultaFin, rol);
            }
            
            log.info("📊 [AttendanceService] Marcaciones encontradas: {}", recordsWithNames.size());
            
            // Crear libro Excel
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Marcaciones");
            
            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 12);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Estilo para título principal - ROJO
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            
            // Estilo para información (etiquetas en negrita)
            CellStyle infoStyle = workbook.createCellStyle();
            Font infoFont = workbook.createFont();
            infoFont.setFontHeightInPoints((short) 10);
            infoFont.setBold(true);
            infoStyle.setFont(infoFont);
            
            // Estilo para valores de información
            CellStyle infoValueStyle = workbook.createCellStyle();
            Font infoValueFont = workbook.createFont();
            infoValueFont.setFontHeightInPoints((short) 10);
            infoValueStyle.setFont(infoValueFont);
            
            int rowNum = 0;
            
            // Título principal - ROJO
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE MARCACIONES - YEGO");
            titleCell.setCellStyle(titleStyle);
            titleRow.setHeightInPoints(25); // Altura del título
            
            // Fusionar celdas para el título
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));
            
            // Información del reporte en formato compacto (3 filas, múltiples columnas)
            LocalDateTime fechaGeneracion = LocalDateTime.now();
            String fechaGeneracionStr = fechaGeneracion.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            String fechaConsultaInicioStr = fechaConsultaInicio.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String fechaConsultaFinStr = fechaConsultaFin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            // Fila 1 de información compacta
            Row infoRow1 = sheet.createRow(rowNum++);
            infoRow1.setHeightInPoints(18);
            infoRow1.createCell(0).setCellValue("Fecha Inicio:");
            infoRow1.getCell(0).setCellStyle(infoStyle);
            infoRow1.createCell(1).setCellValue(fechaConsultaInicioStr);
            infoRow1.getCell(1).setCellStyle(infoValueStyle);
            infoRow1.createCell(2).setCellValue("Fecha Fin:");
            infoRow1.getCell(2).setCellStyle(infoStyle);
            infoRow1.createCell(3).setCellValue(fechaConsultaFinStr);
            infoRow1.getCell(3).setCellStyle(infoValueStyle);
            infoRow1.createCell(4).setCellValue("Total:");
            infoRow1.getCell(4).setCellStyle(infoStyle);
            infoRow1.createCell(5).setCellValue(recordsWithNames.size());
            infoRow1.getCell(5).setCellStyle(infoValueStyle);
            
            // Fila 2 de información compacta
            Row infoRow2 = sheet.createRow(rowNum++);
            infoRow2.setHeightInPoints(18);
            infoRow2.createCell(0).setCellValue("Rol Consultado:");
            infoRow2.getCell(0).setCellStyle(infoStyle);
            String rolDisplay = (rol != null && rol.equalsIgnoreCase("TODOS")) ? "TODOS LOS ROLES" : rol.toUpperCase();
            infoRow2.createCell(1).setCellValue(rolDisplay);
            infoRow2.getCell(1).setCellStyle(infoValueStyle);
            
            // Fila 3 de información compacta
            Row infoRow3 = sheet.createRow(rowNum++);
            infoRow3.setHeightInPoints(18);
            infoRow3.createCell(0).setCellValue("Fecha de Generación:");
            infoRow3.getCell(0).setCellStyle(infoStyle);
            infoRow3.createCell(1).setCellValue(fechaGeneracionStr);
            infoRow3.getCell(1).setCellStyle(infoValueStyle);
            infoRow3.createCell(2).setCellValue("Generado por:");
            infoRow3.getCell(2).setCellStyle(infoStyle);
            infoRow3.createCell(3).setCellValue(rolUsuarioGenerador != null ? rolUsuarioGenerador.toUpperCase() : "N/A");
            infoRow3.getCell(3).setCellStyle(infoValueStyle);
            
            rowNum++; // Un solo espacio antes de los encabezados
            
            // Encabezados - Columnas: ID, Usuario, Email, Rol, Tipo de Marcación, Fecha, Hora
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Usuario");
            headerRow.createCell(2).setCellValue("Email");
            headerRow.createCell(3).setCellValue("Rol");
            headerRow.createCell(4).setCellValue("Tipo de Marcación");
            headerRow.createCell(5).setCellValue("Fecha");
            headerRow.createCell(6).setCellValue("Hora");
            
            // Aplicar estilo a todos los encabezados
            for (int i = 0; i < 7; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }
            
            // Llenar datos
            // El query devuelve: id, user_id, attendance_type, recorded_date, recorded_time, recorded_at, full_name, email, role_name
            for (Object[] recordData : recordsWithNames) {
                try {
                    Row row = sheet.createRow(rowNum++);
                    
                    // Extraer datos del array según el orden del query
                    Long id = ((Number) recordData[0]).longValue();
                    Long empleadoId = ((Number) recordData[1]).longValue();
                    String attendanceType = (String) recordData[2];
                    LocalDate recordedDate = ((java.sql.Date) recordData[3]).toLocalDate();
                    LocalTime recordedTime = ((java.sql.Time) recordData[4]).toLocalTime();
                    
                    // Datos del usuario
                    String fullName = recordData[6] != null ? (String) recordData[6] : "Usuario " + empleadoId;
                    String email = recordData[7] != null ? (String) recordData[7] : "";
                    String roleName = recordData[8] != null ? (String) recordData[8] : "";
                    
                    // Mapear tipo de marcación a español
                    String tipoMarcacion;
                    switch (attendanceType) {
                        case "ENTRY":
                            tipoMarcacion = "ENTRADA";
                            break;
                        case "EXIT_BREAK":
                            tipoMarcacion = "SALIDA REFRIGERIO";
                            break;
                        case "RETURN_BREAK":
                            tipoMarcacion = "REGRESO REFRIGERIO";
                            break;
                        case "EXIT":
                            tipoMarcacion = "SALIDA";
                            break;
                        default:
                            tipoMarcacion = attendanceType;
                    }
                    
                    // Escribir todas las columnas en el orden correcto
                    row.createCell(0).setCellValue(id);
                    row.createCell(1).setCellValue(fullName);
                    row.createCell(2).setCellValue(email);
                    row.createCell(3).setCellValue(roleName);
                    row.createCell(4).setCellValue(tipoMarcacion);
                    row.createCell(5).setCellValue(recordedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    row.createCell(6).setCellValue(recordedTime.toString());
                    
                } catch (Exception e) {
                    log.error("❌ [AttendanceService] Error procesando registro para Excel: {}", e.getMessage());
                }
            }
            
            // Ajustar ancho de columnas - Las 7 columnas
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convertir a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            log.info("✅ [AttendanceService] Excel generado exitosamente con {} registros", recordsWithNames.size());
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("❌ [AttendanceService] Error generando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando Excel", e);
        }
    }
}