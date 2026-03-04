package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalculatedShiftRepository extends JpaRepository<CalculatedShift, Long> {

    List<CalculatedShift> findByDriverIdAndFecha(String driverId, LocalDate fecha);

    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId AND c.fecha = :fecha AND c.esManual = true")
    List<CalculatedShift> findByDriverIdAndFechaAndEsManual(
        @Param("driverId") String driverId,
        @Param("fecha") LocalDate fecha
    );
    
    @Query("SELECT c FROM CalculatedShift c WHERE c.driverId = :driverId ORDER BY c.fecha ASC")
    List<CalculatedShift> findByDriverIdOrderByFecha(@Param("driverId") String driverId);

    @Query("SELECT c FROM CalculatedShift c WHERE c.fecha = :fecha ORDER BY c.driverId ASC")
    List<CalculatedShift> findByFechaOrderByDriverId(@Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM CalculatedShift c WHERE c.pagado = true ORDER BY c.driverId ASC, c.fecha ASC")
    List<CalculatedShift> findByPagadoTrue();

    @Query("SELECT c FROM CalculatedShift c WHERE c.pagado = true AND c.fecha = :fecha ORDER BY c.driverId ASC, c.fecha ASC")
    List<CalculatedShift> findByPagadoTrueAndFecha(@Param("fecha") LocalDate fecha);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CalculatedShift c SET c.pagado = true WHERE c.id IN :ids")
    int markAsPaidByIds(@Param("ids") List<Long> ids);

    @Query("SELECT c.tipoTurno FROM CalculatedShift c WHERE c.id IN :ids")
    List<CalculatedShift.TipoTurno> findTipoTurnoByIdIn(@Param("ids") List<Long> ids);
}

