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

    List<CalculatedShift> findByDriverIdAndFecha(String driverId, LocalDate fecha);

    List<CalculatedShift> findByDriverIdAndFechaAndEstado(String driverId, LocalDate fecha, CalculatedShift.EstadoTurno estado);

    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId AND c.estado = :estado AND c.horaFin IS NULL")
    List<CalculatedShift> findActiveShiftsByDriverId(@Param("driverId") String driverId, @Param("estado") CalculatedShift.EstadoTurno estado);

    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId AND c.fecha = :fecha AND c.horaInicio = :horaInicio")
    Optional<CalculatedShift> findByDriverIdAndFechaAndHoraInicio(
        @Param("driverId") String driverId,
        @Param("fecha") LocalDate fecha,
        @Param("horaInicio") LocalDateTime horaInicio
    );
    
    @Query("SELECT c FROM CalculatedShift c WHERE c.estado = :estado AND c.horaFin IS NULL AND c.fecha = :fecha")
    List<CalculatedShift> findActiveShiftsByFecha(
        @Param("estado") CalculatedShift.EstadoTurno estado,
        @Param("fecha") LocalDate fecha
    );
    
    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId AND c.fecha = :fecha AND c.horaFin IS NULL")
    List<CalculatedShift> findActiveShiftsByDriverIdAndFecha(
        @Param("driverId") String driverId,
        @Param("fecha") LocalDate fecha
    );
    
    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId AND c.fecha = :fecha AND c.esManual = true")
    List<CalculatedShift> findByDriverIdAndFechaAndEsManual(
        @Param("driverId") String driverId,
        @Param("fecha") LocalDate fecha
    );
    
    /**
     * Obtiene todos los CalculatedShift de un driver agrupados por fecha
     * @param driverId ID del conductor
     * @return Lista de CalculatedShift ordenados por fecha
     */
    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId ORDER BY c.fecha ASC")
    List<CalculatedShift> findByDriverIdOrderByFecha(@Param("driverId") String driverId);
    
}

