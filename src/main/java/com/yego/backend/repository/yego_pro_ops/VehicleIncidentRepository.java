package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.VehicleIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleIncidentRepository extends JpaRepository<VehicleIncident, Long> {

    List<VehicleIncident> findByYangoCarIdOrderByFechaDesc(String yangoCarId);

    List<VehicleIncident> findByYangoCarIdAndEstadoOrderByFechaDesc(String yangoCarId, String estado);
}
