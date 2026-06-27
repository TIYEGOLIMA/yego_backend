package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.OperationalShiftSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationalShiftSessionRepository extends JpaRepository<OperationalShiftSession, UUID> {

    @Query("""
        select session from OperationalShiftSession session
        where (:from is null or coalesce(session.closedAt, session.lastActivityAt, session.openedAt) >= :from)
          and (:to is null or session.openedAt <= :to)
          and (:driverId is null or session.driverId = :driverId)
          and (:vehicleKey is null or session.vehicleKey = :vehicleKey)
          and (:state is null or session.state = :state)
        order by session.openedAt desc, session.id desc
        """)
    List<OperationalShiftSession> search(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey,
            @Param("state") String state,
            Pageable pageable);

    @Query("""
        select session from OperationalShiftSession session
        where (:from is null or coalesce(session.closedAt, session.lastActivityAt, session.openedAt) >= :from)
          and (:to is null or session.openedAt <= :to)
          and (:driverId is null or session.driverId = :driverId)
          and (:vehicleKey is null or session.vehicleKey = :vehicleKey)
        order by session.openedAt asc, session.id asc
        """)
    List<OperationalShiftSession> findForValidation(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey);

    @Query("""
        select session from OperationalShiftSession session
        where (:from is null or coalesce(session.closedAt, session.lastActivityAt, session.openedAt) >= :from)
          and (:to is null or session.openedAt <= :to)
          and (:driverId is null or session.driverId = :driverId)
          and (:vehicleKey is null or session.vehicleKey = :vehicleKey)
        order by session.openedAt asc, session.id asc
        """)
    List<OperationalShiftSession> findForReprocess(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId,
            @Param("vehicleKey") String vehicleKey);
}
