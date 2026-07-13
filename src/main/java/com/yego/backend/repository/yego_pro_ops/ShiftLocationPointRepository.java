package com.yego.backend.repository.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.entities.ShiftLocationPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftLocationPointRepository extends JpaRepository<ShiftLocationPoint, UUID> {

    Optional<ShiftLocationPoint> findFirstByShiftSessionIdOrderByRecordedAtDesc(UUID shiftSessionId);

    List<ShiftLocationPoint> findByShiftSessionIdOrderByRecordedAtAsc(UUID shiftSessionId);

    @Query(value = """
        SELECT DISTINCT ON (p.shift_session_id) p.*
        FROM shift_location_points p
        JOIN shift_sessions s ON s.id = p.shift_session_id
        WHERE s.status = 'active'
          AND s.deleted = false
        ORDER BY p.shift_session_id, p.recorded_at DESC
        """, nativeQuery = true)
    List<ShiftLocationPoint> findLatestForActiveShifts();

}
