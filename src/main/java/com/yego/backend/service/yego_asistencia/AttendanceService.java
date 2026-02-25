package com.yego.backend.service.yego_asistencia;

import com.yego.backend.entity.yego_asistencia.entities.AttendanceRecord;
import com.yego.backend.service.yego_asistencia.dto.ExportResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interfaz del servicio de asistencia del sistema YEGO Asistencia
 * Basado en el sistema original de marcador_asistencia
 */
public interface AttendanceService {
    
    // ===== MÉTODOS PRINCIPALES DE MARCACIÓN =====
    
    /**
     * Validar secuencia de marcaciones
     */
    String validateAttendanceSequence(Long userId, String attendanceType);
    
    /**
     * Crear registro de asistencia
     */
    AttendanceRecord createAttendanceRecord(Long userId, String attendanceType, String computerName, String windowsUsername, String localIp, String machineId);
    
    /**
     * Procesar marcación completa con validación y mensaje
     */
    Map<String, Object> processAttendanceMarking(Long userId, Map<String, Object> attendanceData);
    
    // ===== MÉTODOS DE CONSULTA BÁSICA =====
    
    /**
     * Obtener marcaciones del día actual
     */
    List<Map<String, Object>> getTodayAttendances(Long userId);
    
    /**
     * Obtener estadísticas del empleado
     */
    Map<String, Object> getEmployeeStatistics(Long userId);
    
    // ===== MÉTODOS DE VALIDACIÓN =====
    
    /**
     * Verificar si la IP está autorizada
     */
    boolean isAuthorizedIp(String ip);
    
    /**
     * Verificar si el usuario puede marcar entrada
     */
    boolean canMarkEntry(Long userId);
    
    /**
     * Verificar si el usuario puede marcar salida
     */
    boolean canMarkExit(Long userId);
    
    /**
     * Verificar si el usuario está actualmente trabajando
     */
    boolean isUserCurrentlyWorking(Long userId);
    
    /**
     * Verificar si el usuario está en refrigerio
     */
    boolean isUserOnBreak(Long userId);
    
    /**
     * Obtener marcaciones por rango de fechas
     */
    List<Map<String, Object>> getAttendanceRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Procesar marcación con validación de IP
     */
    Map<String, Object> processAttendanceMarkingWithIpValidation(Long userId, Map<String, Object> attendanceData, String clientIp);
    
    /**
     * Obtener marcaciones por rol
     */
    List<Map<String, Object>> getAttendanceRecordsByRole(Long userId, String userRole, LocalDate fecha);

    /**
     * Obtener marcaciones por rol; si fechaParam es null o vacío usa la fecha actual (Perú).
     */
    List<Map<String, Object>> getAttendanceRecordsByRole(Long userId, String userRole, String fechaParam);

    /**
     * Obtener usuarios para lista de asistencias: ADMIN/SUPERADMIN ven todos;
     * si userId es manager_id de un área, solo colaboradores de esa área; resto lista vacía.
     */
    List<Map<String, Object>> getUsersByRole(Long userId, String userRole);

    /**
     * Obtener marcaciones por rango de fechas (validación de fechas en servicio).
     * @return Map con success, marcaciones, message (si error), fechaInicio, fechaFin
     */
    Map<String, Object> getAttendanceRecordsByDateRangeValidated(Long userId, String fechaInicio, String fechaFin);

    /**
     * Respuesta para verificación de IP (ipValida, ip, mensaje).
     */
    Map<String, Object> verifyIpResponse(String ip);

    /**
     * Exportar marcaciones a Excel por rango de fechas y rol.
     * @return ExportResult con content y fileName; hasContent() false si no hay datos
     */
    ExportResult exportarMarcacionesPorRangoDeFechasYRol(String fechaInicio, String fechaFin, String rol, String rolUsuarioGenerador);
}

