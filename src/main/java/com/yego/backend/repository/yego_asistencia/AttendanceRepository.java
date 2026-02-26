package com.yego.backend.repository.yego_asistencia;

import com.yego.backend.entity.yego_asistencia.entities.AttendanceRecord;
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
     * Obtener marcaciones por rango de fechas (usuario)
     */
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.userId = :userId AND ar.recordedDate BETWEEN :startDate AND :endDate ORDER BY ar.recordedAt ASC")
    List<AttendanceRecord> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);
    
    /**
     * Obtener todos los usuarios con el nombre del rol y del área (si existe tabla areas)
     */
    @Query(value = "SELECT u.id, u.name, u.last_name, r.name as role, u.email, a.name as area_name " +
                   "FROM users u " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "LEFT JOIN areas a ON u.area_id = a.id " +
                   "ORDER BY u.name ASC", nativeQuery = true)
    List<Object[]> findAllUsers();

    /**
     * Usuarios de un área (colaboradores) para lista de asistencias cuando el jefe es manager de esa área.
     */
    @Query(value = "SELECT u.id, u.name, u.last_name, r.name as role, u.email, a.name as area_name " +
                   "FROM users u " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "LEFT JOIN areas a ON u.area_id = a.id " +
                   "WHERE u.area_id = :areaId AND u.active = true " +
                   "ORDER BY u.name ASC", nativeQuery = true)
    List<Object[]> findUsersByAreaId(@Param("areaId") Long areaId);

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
    
    /**
     * Obtener todas las marcaciones por rango de fechas sin filtrar por rol (para TODOS)
     * Excluye los roles: TABLET1, TABLET2, PRINCIPAL
     */
    @Query(value = "SELECT ar.id, ar.user_id, ar.attendance_type, ar.recorded_date, ar.recorded_time, ar.recorded_at, " +
                   "COALESCE(CONCAT(u.name, ' ', u.last_name), 'Usuario ' || ar.user_id) as full_name, " +
                   "u.email, r.name as role_name " +
                   "FROM module_attendance_records ar " +
                   "LEFT JOIN users u ON ar.user_id = u.id " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "WHERE ar.recorded_date BETWEEN :fechaInicio AND :fechaFin " +
                   "AND LOWER(r.name) NOT IN ('tablet1', 'tablet2', 'principal') " +
                   "ORDER BY u.name ASC, ar.recorded_at ASC", nativeQuery = true)
    List<Object[]> findByDateRangeAllRoles(@Param("fechaInicio") LocalDate fechaInicio, 
                                            @Param("fechaFin") LocalDate fechaFin);

    /**
     * Marcaciones por rango de fechas, rol y lista de user IDs (ej. colaboradores del área + jefe).
     */
    @Query(value = "SELECT ar.id, ar.user_id, ar.attendance_type, ar.recorded_date, ar.recorded_time, ar.recorded_at, " +
                   "COALESCE(CONCAT(u.name, ' ', u.last_name), 'Usuario ' || ar.user_id) as full_name, " +
                   "u.email, r.name as role_name " +
                   "FROM module_attendance_records ar " +
                   "LEFT JOIN users u ON ar.user_id = u.id " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "WHERE ar.recorded_date BETWEEN :fechaInicio AND :fechaFin " +
                   "AND LOWER(r.name) = LOWER(:rol) AND ar.user_id IN (:userIds) " +
                   "ORDER BY u.name ASC, ar.recorded_at ASC", nativeQuery = true)
    List<Object[]> findByDateRangeAndRoleAndUserIdIn(@Param("fechaInicio") LocalDate fechaInicio,
                                                      @Param("fechaFin") LocalDate fechaFin,
                                                      @Param("rol") String rol,
                                                      @Param("userIds") List<Long> userIds);

    /**
     * Marcaciones por rango de fechas (todos los roles) y lista de user IDs (ej. colaboradores del área + jefe).
     */
    @Query(value = "SELECT ar.id, ar.user_id, ar.attendance_type, ar.recorded_date, ar.recorded_time, ar.recorded_at, " +
                   "COALESCE(CONCAT(u.name, ' ', u.last_name), 'Usuario ' || ar.user_id) as full_name, " +
                   "u.email, r.name as role_name " +
                   "FROM module_attendance_records ar " +
                   "LEFT JOIN users u ON ar.user_id = u.id " +
                   "LEFT JOIN roles r ON u.role = r.id " +
                   "WHERE ar.recorded_date BETWEEN :fechaInicio AND :fechaFin " +
                   "AND LOWER(r.name) NOT IN ('tablet1', 'tablet2', 'principal') AND ar.user_id IN (:userIds) " +
                   "ORDER BY u.name ASC, ar.recorded_at ASC", nativeQuery = true)
    List<Object[]> findByDateRangeAllRolesAndUserIdIn(@Param("fechaInicio") LocalDate fechaInicio,
                                                       @Param("fechaFin") LocalDate fechaFin,
                                                       @Param("userIds") List<Long> userIds);
}

