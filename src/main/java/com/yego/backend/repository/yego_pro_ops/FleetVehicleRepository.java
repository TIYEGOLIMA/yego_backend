package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.FleetVehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, String> {

    @EntityGraph(attributePaths = "segment")
    List<FleetVehicle> findByActivoTrue();

    @EntityGraph(attributePaths = "segment")
    List<FleetVehicle> findBySegment_IdAndActivoTrue(UUID segmentId);

    long countBySegment_IdAndActivoTrue(UUID segmentId);

    @EntityGraph(attributePaths = "segment")
    Optional<FleetVehicle> findFirstByNumberIgnoreCaseAndActivoTrueOrderByUpdatedAtDesc(String number);
}
