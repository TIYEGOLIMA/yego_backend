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

    // ── Listado (excluye vehículos inactivos: status_id <> statusIdExcluido) ──

    @EntityGraph(attributePaths = "segment")
    List<FleetVehicle> findByActivoTrueAndStatusIdNot(String statusIdExcluido);

    @EntityGraph(attributePaths = "segment")
    List<FleetVehicle> findBySegment_IdAndActivoTrueAndStatusIdNot(UUID segmentId, String statusIdExcluido);

    long countBySegment_IdAndActivoTrueAndStatusIdNot(UUID segmentId, String statusIdExcluido);

    @EntityGraph(attributePaths = "segment")
    List<FleetVehicle> findTop20ByNumberContainingIgnoreCaseAndActivoTrueAndStatusIdNotOrderByNumberAsc(String number, String statusIdExcluido);

    @EntityGraph(attributePaths = "segment")
    Optional<FleetVehicle> findByYangoCarIdAndActivoTrueAndStatusIdNot(String yangoCarId, String statusIdExcluido);

    // ── Otros (sin filtro de estado) ──

    @EntityGraph(attributePaths = "segment")
    Optional<FleetVehicle> findFirstByNumberIgnoreCaseAndActivoTrueOrderByUpdatedAtDesc(String number);
}
