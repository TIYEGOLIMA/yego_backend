package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AreaTaskRepository extends JpaRepository<AreaTask, Long> {

    List<AreaTask> findByAreaIdInOrderByAreaIdAscSortOrderAscIdAsc(Collection<Long> areaIds);

    long countByAreaIdIn(Collection<Long> areaIds);

    long countByAreaIdInAndStatus(Collection<Long> areaIds, AreaTaskStatus status);
}
