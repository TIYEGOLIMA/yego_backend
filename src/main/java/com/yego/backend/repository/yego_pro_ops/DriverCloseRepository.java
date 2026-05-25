package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverCloseRepository extends JpaRepository<DriverClose, Long> {

    Optional<DriverClose> findFirstByDriverIdAndFechaOrderByIdDesc(String driverId, LocalDate fecha);

    Optional<DriverClose> findFirstByDriverIdOrderByIdDesc(String driverId);

    List<DriverClose> findByDriverIdAndFechaBetween(String driverId, LocalDate inicio, LocalDate fin);

    List<DriverClose> findByFechaBetween(LocalDate inicio, LocalDate fin);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM DriverClose d WHERE d.driverId = :driverId AND d.fecha = :fecha")
    int deleteByDriverIdAndFecha(@org.springframework.data.repository.query.Param("driverId") String driverId, @org.springframework.data.repository.query.Param("fecha") LocalDate fecha);
}
