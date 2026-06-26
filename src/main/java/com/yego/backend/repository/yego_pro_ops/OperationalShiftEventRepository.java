package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OperationalShiftEventRepository extends JpaRepository<OperationalShiftEvent, UUID> {

    @Query("""
        select event from OperationalShiftEvent event
        where (:from is null or event.eventTime >= :from)
          and (:to is null or event.eventTime <= :to)
          and (:shiftId is null or event.operationalShiftSessionId = :shiftId)
          and (:driverId is null or event.driverId = :driverId)
          and (:vehicleKey is null or event.vehicleKey = :vehicleKey)
        order by event.eventTime asc, event.id asc
        """)
    List<OperationalShiftEvent> search(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("shiftId") UUID shiftId,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey);

    @Modifying
    void deleteByOperationalShiftSessionIdIn(Collection<UUID> operationalShiftSessionIds);

    @Modifying
    @Query("""
        delete from OperationalShiftEvent event
        where event.operationalShiftSessionId is null
          and event.eventType <> 'TRIP_FACT_UPSERTED'
          and (:from is null or event.eventTime >= :from)
          and (:to is null or event.eventTime <= :to)
          and (:driverId is null or event.driverId = :driverId)
          and (:vehicleKey is null or event.vehicleKey = :vehicleKey)
        """)
    void deleteStandaloneInferenceEvents(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey);
}
