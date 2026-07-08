package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftSessionRepository extends JpaRepository<ShiftSession, UUID> {

    Optional<ShiftSession> findByDriverIdAndStatusAndDeletedFalse(String driverId, String status);

    @Query("SELECT s FROM ShiftSession s WHERE s.driverId = :driverId AND s.status = 'settled' AND s.deleted = false ORDER BY s.closedAt DESC LIMIT 1")
    Optional<ShiftSession> findTopByDriverIdAndStatusOrderByClosedAtDesc(@Param("driverId") String driverId);

    @Query("SELECT s FROM ShiftSession s WHERE s.driverId = :driverId AND s.deleted = false ORDER BY s.startedAt DESC")
    List<ShiftSession> findByDriverIdOrderByStartedAtDesc(@Param("driverId") String driverId);

    @Query("SELECT MAX(s.settledAt) FROM ShiftSession s WHERE s.driverId = :driverId AND s.status = 'settled' AND s.deleted = false")
    Optional<java.time.LocalDateTime> findLastSettledAtByDriverId(@Param("driverId") String driverId);

    @Query("SELECT MAX(s.closedAt) FROM ShiftSession s WHERE s.driverId = :driverId AND s.status IN ('closed','settled') AND s.deleted = false")
    Optional<java.time.LocalDateTime> findLastClosedAtByDriverId(@Param("driverId") String driverId);

    @Query("SELECT COUNT(s) > 0 FROM ShiftSession s WHERE s.driverId = :driverId AND s.status IN ('closed', 'settled') AND s.deleted = false AND s.startedAt < :hasta AND (s.closedAt IS NULL OR s.closedAt > :desde)")
    boolean existsOverlapping(@Param("driverId") String driverId, @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta);

    @Query("""
        SELECT s FROM ShiftSession s
        WHERE s.deleted = false
          AND s.status = 'closed'
          AND s.closedAt IS NOT NULL
          AND s.closedAt >= :desde
          AND s.closedAt <= :hasta
          AND (:driverId IS NULL OR :driverId = '' OR s.driverId = :driverId)
        ORDER BY s.closedAt DESC
        """)
    List<ShiftSession> findClosedForExternalConsult(
            @Param("driverId") String driverId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
