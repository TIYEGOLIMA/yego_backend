package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectIdOrderByStartDateAsc(Long projectId);
}
