package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationalValidationShiftSessionReadRepository extends JpaRepository<ShiftSession, UUID> {

    @Query("""
        select session from ShiftSession session
        where session.deleted = false
          and (:from is null or coalesce(session.closedAt, session.startedAt) >= :from)
          and (:to is null or session.startedAt <= :to)
          and (:driverId is null or session.driverId = :driverId)
        order by session.startedAt asc, session.id asc
        """)
    List<ShiftSession> findForValidation(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("driverId") String driverId);
}
