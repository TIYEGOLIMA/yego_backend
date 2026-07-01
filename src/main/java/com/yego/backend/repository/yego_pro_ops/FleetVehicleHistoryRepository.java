package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.FleetVehicleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FleetVehicleHistoryRepository extends JpaRepository<FleetVehicleHistory, UUID> {

    List<FleetVehicleHistory> findByYangoCarIdOrderByCreatedAtDesc(String yangoCarId);
}
