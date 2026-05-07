package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTaskSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaTaskSubtaskRepository extends JpaRepository<AreaTaskSubtask, Long> {

    @Query("""
            SELECT COALESCE(MAX(s.sortOrder), 0) FROM AreaTaskSubtask s WHERE s.parentTaskId = :parentId
            """)
    int findMaxSortOrderByParentTaskId(@Param("parentId") Long parentId);

    /**
     * Progreso ponderado padre (0–100), mismo criterio que antes en Java.
     * Sin filas para el padre devuelve null.
     */
    @Query(value = """
            SELECT CAST(LEAST(100, GREATEST(0,
              ROUND((COALESCE(SUM(CASE WHEN s.done THEN s.weight ELSE 0 END), 0)
                / NULLIF(SUM(s.weight), 0)) * 100, 0)
            )) AS INTEGER)
            FROM area_task_subtasks s
            WHERE s.parent_task_id = :parentId
            GROUP BY s.parent_task_id
            """, nativeQuery = true)
    Integer computeWeightedProgressPercent(@Param("parentId") Long parentId);

    List<AreaTaskSubtask> findByParentTaskIdOrderBySortOrderAscIdAsc(Long parentTaskId);

    boolean existsByParentTaskIdAndAssignedUserId(Long parentTaskId, Long assignedUserId);

    @Query("SELECT DISTINCT s.parentTaskId FROM AreaTaskSubtask s WHERE s.assignedUserId = :userId")
    List<Long> findDistinctParentTaskIdsByAssignedUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AreaTaskSubtask s WHERE s.id = :subtaskId AND s.parentTaskId = :parentTaskId")
    int deleteByIdAndParentTaskId(@Param("subtaskId") Long subtaskId, @Param("parentTaskId") Long parentTaskId);

    long countByParentTaskId(Long parentTaskId);

    boolean existsByIdAndParentTaskId(Long id, Long parentTaskId);

    @Query(value = """
            SELECT parent_task_id AS pid,
                   COUNT(*)::int AS total,
                   COALESCE(SUM(CASE WHEN done THEN 1 ELSE 0 END), 0)::int AS done_n
            FROM area_task_subtasks
            WHERE parent_task_id IN (:ids)
            GROUP BY parent_task_id
            """, nativeQuery = true)
    List<Object[]> summarizeByParentTaskIds(@Param("ids") List<Long> ids);
}
