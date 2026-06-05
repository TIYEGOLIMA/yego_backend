package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftSessionRepository extends JpaRepository<ShiftSession, UUID> {

    Optional<ShiftSession> findByDriverIdAndStatus(String driverId, String status);

    @Query("SELECT s FROM ShiftSession s WHERE s.driverId = :driverId AND s.status = 'settled' ORDER BY s.closedAt DESC")
    List<ShiftSession> findSettledByDriverIdOrderByClosedAtDesc(@Param("driverId") String driverId);

    @Query("SELECT s FROM ShiftSession s WHERE s.driverId = :driverId AND s.status = 'settled' ORDER BY s.closedAt DESC LIMIT 1")
    Optional<ShiftSession> findTopByDriverIdAndStatusOrderByClosedAtDesc(@Param("driverId") String driverId, @Param("status") String status);

    @Query("SELECT s FROM ShiftSession s WHERE s.driverId = :driverId ORDER BY s.startedAt DESC")
    List<ShiftSession> findByDriverIdOrderByStartedAtDesc(@Param("driverId") String driverId);

    boolean existsByDriverIdAndStatus(String driverId, String status);
}
