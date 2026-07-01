package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, String> {

    List<FleetVehicle> findByActivoTrue();

    List<FleetVehicle> findBySegment_IdAndActivoTrue(UUID segmentId);

    long countBySegment_IdAndActivoTrue(UUID segmentId);

    Optional<FleetVehicle> findByNumberIgnoreCaseAndActivoTrue(String number);
}
