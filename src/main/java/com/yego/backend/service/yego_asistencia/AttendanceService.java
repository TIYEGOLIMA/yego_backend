package com.yego.backend.service.yego_asistencia;

import com.yego.backend.entity.yego_asistencia.entities.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    
    /**
     * Obtener última marcación del usuario
     */
    Optional<AttendanceRecord> getLastAttendanceRecord(Long userId);
    
    /**
     * Obtener registros de asistencia por usuario y fecha
     */
    List<AttendanceRecord> getAttendanceRecordsByUserAndDate(Long userId, LocalDate date);
    
    /**
     * Obtener registros de asistencia por usuario y rango de fechas
     */
    List<AttendanceRecord> getAttendanceRecordsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    
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
    
    // ===== MÉTODOS DE ESTADÍSTICAS =====
    
    /**
     * Obtener tiempo trabajado del día
     */
    Map<String, Object> getWorkedTimeToday(Long userId);
    
    /**
     * Obtener tiempo trabajado en rango de fechas
     */
    Map<String, Object> getWorkedTimeInRange(Long userId, LocalDate startDate, LocalDate endDate);
    
    // ===== MÉTODOS DE ADMINISTRACIÓN =====
    
    /**
     * Obtener todos los registros de asistencia con paginación
     */
    Page<AttendanceRecord> getAllAttendanceRecords(Pageable pageable);
    
    /**
     * Obtener registro de asistencia por ID
     */
    Optional<AttendanceRecord> getAttendanceRecordById(Long id);
    
    /**
     * Actualizar registro de asistencia
     */
    AttendanceRecord updateAttendanceRecord(Long id, AttendanceRecord attendanceRecord);
    
    /**
     * Eliminar registro de asistencia
     */
    void deleteAttendanceRecord(Long id);
    
    /**
     * Exportar registros de asistencia
     */
    byte[] exportAttendanceRecords(LocalDate startDate, LocalDate endDate, String format);
    
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
     * Obtener usuarios por rol
     */
    List<Map<String, Object>> getUsersByRole(String userRole);
    
    /**
     * Exportar marcaciones a Excel por fecha y rol
     * @param fecha Fecha en formato YYYY-MM-DD
     * @param rol Nombre del rol
     * @param rolUsuarioGenerador Rol del usuario que genera el reporte
     * @return Byte array del archivo Excel
     * @throws IllegalArgumentException si la fecha es inválida
     */
    byte[] exportarMarcacionesPorFechaYRol(String fecha, String rol, String rolUsuarioGenerador);
}

