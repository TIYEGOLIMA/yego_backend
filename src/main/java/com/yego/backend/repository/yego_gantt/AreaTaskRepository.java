package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.AreaTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AreaTaskRepository extends JpaRepository<AreaTask, Long> {

    @Query("""
        SELECT t FROM AreaTask t
        WHERE (:areaIdFilter IS NULL OR t.areaId = :areaIdFilter)
          AND (:projectIdFilter IS NULL OR t.projectId = :projectIdFilter)
          AND (:priorityFilter IS NULL OR t.priority = :priorityFilter)
        ORDER BY t.areaId ASC, t.sortOrder ASC, t.id ASC
        """)
    List<AreaTask> findAdminFiltered(
            @Param("areaIdFilter") Long areaIdFilter,
            @Param("projectIdFilter") Long projectIdFilter,
            @Param("priorityFilter") AreaTaskPriority priorityFilter);

    @Query("""
        SELECT t FROM AreaTask t
        WHERE t.areaId IN :areaIds
          AND (:areaIdFilter IS NULL OR t.areaId = :areaIdFilter)
          AND (:projectIdFilter IS NULL OR t.projectId = :projectIdFilter)
          AND (:priorityFilter IS NULL OR t.priority = :priorityFilter)
        ORDER BY t.areaId ASC, t.sortOrder ASC, t.id ASC
        """)
    List<AreaTask> findScopedFiltered(
            @Param("areaIds") Collection<Long> areaIds,
            @Param("areaIdFilter") Long areaIdFilter,
            @Param("projectIdFilter") Long projectIdFilter,
            @Param("priorityFilter") AreaTaskPriority priorityFilter);

    List<AreaTask> findByAreaIdInOrderByAreaIdAscSortOrderAscIdAsc(Collection<Long> areaIds);

    @Query("""
        SELECT DISTINCT t.projectId FROM AreaTask t
        WHERE t.projectId IS NOT NULL AND t.areaId IN :areaIds
        """)
    List<Long> findDistinctProjectIdsByAreaIdIn(@Param("areaIds") Collection<Long> areaIds);

    List<AreaTask> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

    long countBySprintId(Long sprintId);

    long countBySprintIdAndStatus(Long sprintId, AreaTaskStatus status);
}
