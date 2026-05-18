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
}
