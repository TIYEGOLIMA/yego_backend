package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.DriverClose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverCloseRepository extends JpaRepository<DriverClose, Long> {

    /**
     * 📋 VISTA: DetalleView
     * Obtiene el cierre más reciente de un driver para una fecha específica
     */
    Optional<DriverClose> findFirstByDriverIdAndFechaOrderByIdDesc(String driverId, LocalDate fecha);
}

