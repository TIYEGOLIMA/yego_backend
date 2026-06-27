package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OperationalValidationShiftSampleReadRepository extends JpaRepository<ShiftSession, UUID> {

    @Query("""
            SELECT s
            FROM ShiftSession s
            WHERE s.deleted = false
              AND s.startedAt >= :from
              AND s.startedAt < :to
            ORDER BY s.startedAt DESC, s.id DESC
            """)
    List<ShiftSession> findForSampleSelection(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT MAX(s.startedAt)
            FROM ShiftSession s
            WHERE s.deleted = false
            """)
    LocalDateTime findLatestStartedAt();
}
