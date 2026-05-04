package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.WorkosMeetingMinuteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkosMeetingMinuteItemRepository extends JpaRepository<WorkosMeetingMinuteItem, Long> {

    List<WorkosMeetingMinuteItem> findByMeetingMinute_IdOrderByItemOrderAsc(Long meetingMinuteId);

    @Query("SELECT COALESCE(MAX(i.itemOrder), 0) FROM WorkosMeetingMinuteItem i WHERE i.meetingMinute.id = :minuteId")
    int maxItemOrder(@Param("minuteId") Long minuteId);

    @Query("""
            SELECT i FROM WorkosMeetingMinuteItem i
            JOIN FETCH i.meetingMinute m
            WHERE i.convertedTaskId IS NULL
              AND m.deleted = false
              AND m.status <> :cancelled
            ORDER BY m.meetingDate DESC, i.id ASC
            """)
    List<WorkosMeetingMinuteItem> findAllUnconverted(
            @Param("cancelled") com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus cancelled);

    Optional<WorkosMeetingMinuteItem> findByIdAndMeetingMinute_Id(Long itemId, Long meetingMinuteId);
}
