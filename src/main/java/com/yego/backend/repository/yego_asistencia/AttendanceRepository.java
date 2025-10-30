package com.yego.backend.repository.yego_asistencia;

import com.yego.backend.entity.yego_asistencia.entities.AttendanceRecord;
import com.yego.backend.entity.yego_asistencia.entities.AttendanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    
    /**
     * Obtener todas las marcaciones de un usuario para el día actual
     */
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate = :date ORDER BY ar.recordedAt ASC")
    List<AttendanceRecord> findByUserIdAndRecordedDateOrderByRecordedAtAsc(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    /**
     * Obtener la última marcación de un usuario para el día actual
     */
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate = :date ORDER BY ar.recordedAt DESC")
    List<AttendanceRecord> findByUserIdAndRecordedDateOrderByRecordedAtDesc(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    /**
     * Obtener marcaciones por tipo y usuario
     */
    List<AttendanceRecord> findByUserIdAndAttendanceType(Long userId, AttendanceType attendanceType);
    
    /**
     * Obtener marcaciones por rango de fechas
     */
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate BETWEEN :startDate AND :endDate ORDER BY ar.recordedAt ASC")
    List<AttendanceRecord> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    /**
     * Contar marcaciones de un usuario en un día específico
     */
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate = :date")
    Long countByUserIdAndRecordedDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    /**
     * Verificar si existe una marcación de entrada para el día
     */
    @Query("SELECT COUNT(ar) > 0 FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate = :date AND ar.attendanceType = 'ENTRY'")
    boolean existsEntryForUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    /**
     * Verificar si existe una marcación de salida para el día
     */
    @Query("SELECT COUNT(ar) > 0 FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate = :date AND ar.attendanceType = 'EXIT'")
    boolean existsExitForUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    /**
     * Obtener registros por rango de fechas
     */
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.recordedDate BETWEEN :startDate AND :endDate ORDER BY ar.recordedAt ASC")
    List<AttendanceRecord> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
        /**
         * Obtener registros por fecha específica
         */
        @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.recordedDate = :date ORDER BY ar.recordedAt ASC")
        List<AttendanceRecord> findByRecordedDateOrderByRecordedAtAsc(@Param("date") LocalDate date);
        
    /**
     * Obtener registros por tipo de asistencia
     */
    List<AttendanceRecord> findByAttendanceType(AttendanceType attendanceType);
    
    /**
     * Obtener todos los usuarios con el nombre del rol desde la tabla roles
     */
    @Query(value = "SELECT u.id, u.name, u.last_name, r.name as role, u.email " +
                   "FROM users u " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "ORDER BY u.name ASC", nativeQuery = true)
    List<Object[]> findAllUsers();
    
    /**
     * Obtener marcaciones por usuario y fecha específica con nombres
     */
    @Query(value = "SELECT ar.*, COALESCE(CONCAT(u.name, ' ', u.last_name), 'Usuario ' || ar.user_id) as full_name FROM module_attendance_records ar " +
                   "LEFT JOIN users u ON ar.user_id = u.id " +
                   "WHERE ar.user_id = :userId AND ar.recorded_date = :fecha " +
                   "ORDER BY ar.recorded_at ASC", nativeQuery = true)
    List<Object[]> findByUserIdAndDateWithUserNames(@Param("userId") Long userId, @Param("fecha") LocalDate fecha);

    
    /**
     * Obtener marcaciones por rango de fechas y rol con información del usuario
     */
    @Query(value = "SELECT ar.id, ar.user_id, ar.attendance_type, ar.recorded_date, ar.recorded_time, ar.recorded_at, " +
                   "COALESCE(CONCAT(u.name, ' ', u.last_name), 'Usuario ' || ar.user_id) as full_name, " +
                   "u.email, r.name as role_name " +
                   "FROM module_attendance_records ar " +
                   "LEFT JOIN users u ON ar.user_id = u.id " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "WHERE ar.recorded_date BETWEEN :fechaInicio AND :fechaFin " +
                   "AND LOWER(r.name) = LOWER(:rol) " +
                   "ORDER BY u.name ASC, ar.recorded_at ASC", nativeQuery = true)
    List<Object[]> findByDateRangeAndRole(@Param("fechaInicio") LocalDate fechaInicio, 
                                          @Param("fechaFin") LocalDate fechaFin, 
                                          @Param("rol") String rol);
}

