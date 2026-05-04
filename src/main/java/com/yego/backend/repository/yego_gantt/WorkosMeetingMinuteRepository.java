package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.WorkosMeetingMinute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkosMeetingMinuteRepository extends JpaRepository<WorkosMeetingMinute, Long>,
        JpaSpecificationExecutor<WorkosMeetingMinute> {

    @Query("SELECT DISTINCT m FROM WorkosMeetingMinute m LEFT JOIN FETCH m.items WHERE m.id = :id AND m.deleted = false")
    Optional<WorkosMeetingMinute> findByIdWithItems(@Param("id") Long id);
}
