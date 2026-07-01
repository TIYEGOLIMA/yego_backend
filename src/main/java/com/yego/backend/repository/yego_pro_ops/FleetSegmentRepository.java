package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.FleetSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FleetSegmentRepository extends JpaRepository<FleetSegment, UUID> {

    List<FleetSegment> findByActivoTrue();

    Optional<FleetSegment> findByParkId(String parkId);

    boolean existsByParkId(String parkId);
}
