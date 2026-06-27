package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.OperationalTripFact;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalTripFactRepository extends JpaRepository<OperationalTripFact, UUID> {

    Optional<OperationalTripFact> findByExternalTripId(String externalTripId);

    @Query("""
        select fact from OperationalTripFact fact
        where (:from is null or coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) >= :from)
          and (:to is null or coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) <= :to)
          and (:driverId is null or fact.driverId = :driverId)
          and (:vehicleKey is null or fact.vehicleKey = :vehicleKey)
          and (:status is null or lower(fact.tripStatus) = lower(:status))
        order by coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) asc, fact.externalTripId asc
        """)
    List<OperationalTripFact> search(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
        select fact from OperationalTripFact fact
        where (:from is null or coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) >= :from)
          and (:to is null or coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) <= :to)
          and (:driverId is null or fact.driverId = :driverId)
          and (:vehicleKey is null or fact.vehicleKey = :vehicleKey)
        order by fact.vehicleKey asc, coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) asc, fact.externalTripId asc
        """)
    List<OperationalTripFact> findForInference(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey);

    @Query("""
        select fact from OperationalTripFact fact
        where (:from is null or coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) >= :from)
          and (:to is null or coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) <= :to)
          and (:driverId is null or fact.driverId = :driverId)
          and (:vehicleKey is null or fact.vehicleKey = :vehicleKey)
        order by coalesce(fact.bookedAt, fact.endedAt, fact.observedAt) asc, fact.externalTripId asc
        """)
    List<OperationalTripFact> findForValidation(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey);
}
