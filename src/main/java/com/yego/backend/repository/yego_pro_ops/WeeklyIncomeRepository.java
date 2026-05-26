package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.WeeklyIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyIncomeRepository extends JpaRepository<WeeklyIncome, Long> {

    Optional<WeeklyIncome> findByDriverIdAndFechaInicioAndFechaFin(
            String driverId, LocalDate fechaInicio, LocalDate fechaFin);
}
