package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalculatedShiftRepository extends JpaRepository<CalculatedShift, Long> {

    /**
     * 🔧 USO INTERNO: CalculatedShiftService
     * Obtiene todos los CalculatedShift de un driver para una fecha específica
     */
    List<CalculatedShift> findByDriverIdAndFecha(String driverId, LocalDate fecha);
    
    /**
     * 🔧 USO INTERNO: CalculatedShiftService
     * Obtiene turnos manuales de un driver para una fecha específica
     */
    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId AND c.fecha = :fecha AND c.esManual = true")
    List<CalculatedShift> findByDriverIdAndFechaAndEsManual(
        @Param("driverId") String driverId,
        @Param("fecha") LocalDate fecha
    );
    
    /**
     * 📋 VISTA: DetalleView
     * Obtiene todos los CalculatedShift de un driver agrupados por fecha
     * @param driverId ID del conductor
     * @return Lista de CalculatedShift ordenados por fecha
     */
    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId ORDER BY c.fecha ASC")
    List<CalculatedShift> findByDriverIdOrderByFecha(@Param("driverId") String driverId);
    
    /**
     * 📋 VISTA: Resumen de Pagos
     * Obtiene todos los CalculatedShift agrupados por driver_id
     * @return Lista de CalculatedShift ordenados por driver_id y fecha
     */
    @Query("SELECT c FROM CalculatedShift c ORDER BY c.driverId ASC, c.fecha ASC")
    List<CalculatedShift> findAllOrderByDriverIdAndFecha();
    
    /**
     * 📋 VISTA: Resumen de Pagos
     * Obtiene todos los CalculatedShift de una fecha específica agrupados por driver_id
     * @param fecha Fecha para filtrar los turnos
     * @return Lista de CalculatedShift ordenados por driver_id
     */
    @Query("SELECT c FROM CalculatedShift c WHERE c.fecha = :fecha ORDER BY c.driverId ASC")
    List<CalculatedShift> findByFechaOrderByDriverId(@Param("fecha") LocalDate fecha);
    
    /**
     * 💰 VISTA: Lista de Turnos Pagados
     * Obtiene todos los CalculatedShift pagados (pagado = true)
     * @return Lista de CalculatedShift pagados ordenados por driver_id y fecha
     */
    @Query("SELECT c FROM CalculatedShift c WHERE c.pagado = true ORDER BY c.driverId ASC, c.fecha ASC")
    List<CalculatedShift> findByPagadoTrue();
    
    /**
     * 💰 VISTA: Lista de Turnos Pagados
     * Obtiene todos los CalculatedShift pagados (pagado = true) para una fecha específica
     * @param fecha Fecha para filtrar los turnos pagados
     * @return Lista de CalculatedShift pagados ordenados por driver_id y fecha
     */
    @Query("SELECT c FROM CalculatedShift c WHERE c.pagado = true AND c.fecha = :fecha ORDER BY c.driverId ASC, c.fecha ASC")
    List<CalculatedShift> findByPagadoTrueAndFecha(@Param("fecha") LocalDate fecha);
    
}

