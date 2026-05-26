package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.FacturacionSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturacionSemanalRepository extends JpaRepository<FacturacionSemanal, Long> {

    Optional<FacturacionSemanal> findByDriverIdAndFechaInicioAndFechaFin(
        String driverId, LocalDate fechaInicio, LocalDate fechaFin);

    List<FacturacionSemanal> findByFechaInicioAndFechaFin(LocalDate fechaInicio, LocalDate fechaFin);

    @Modifying
    @Transactional
    @Query("DELETE FROM FacturacionSemanal f WHERE f.fechaInicio = :inicio AND f.fechaFin = :fin")
    void deleteByFechaInicioAndFechaFin(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    List<FacturacionSemanal> findByDriverIdOrderByFechaInicioDesc(String driverId);

    @Query("SELECT f FROM FacturacionSemanal f WHERE f.fechaInicio BETWEEN :inicio AND :fin ORDER BY f.driverId, f.fechaInicio DESC")
    List<FacturacionSemanal> findByRangoFechas(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    List<FacturacionSemanal> findAllByOrderByFechaInicioDesc();
}
