package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.VehicleMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {

    List<VehicleMaintenance> findByYangoCarIdOrderByFechaDesc(String yangoCarId);

    List<VehicleMaintenance> findByYangoCarIdAndTipoOrderByFechaDesc(String yangoCarId, String tipo);

    List<VehicleMaintenance> findByYangoCarIdAndEstadoOrderByFechaDesc(String yangoCarId, String estado);
}
