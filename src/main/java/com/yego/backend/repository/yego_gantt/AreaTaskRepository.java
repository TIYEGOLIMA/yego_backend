package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskPriority;
import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
          AND (
            (:onlyWithoutWorkspace = TRUE AND t.workspaceId IS NULL)
            OR (:onlyWithoutWorkspace = FALSE AND (:workspaceIdFilter IS NULL OR t.workspaceId = :workspaceIdFilter))
          )
          AND (:priorityFilter IS NULL OR t.priority = :priorityFilter)
          AND (:ownerUserIdFilter IS NULL OR t.assignedUserId = :ownerUserIdFilter
              OR :ownerUserIdFilter MEMBER OF t.assignedUserIds)
          AND (:skipPrivateVisibilityFilter = true OR t.privateTask = false
              OR (t.createdByUserId IS NOT NULL AND t.createdByUserId = :viewerUserId))
        ORDER BY t.areaId ASC, t.sortOrder ASC, t.id ASC
        """)
    List<AreaTask> findAdminFiltered(
            @Param("areaIdFilter") Long areaIdFilter,
            @Param("workspaceIdFilter") Long workspaceIdFilter,
            @Param("onlyWithoutWorkspace") boolean onlyWithoutWorkspace,
            @Param("priorityFilter") AreaTaskPriority priorityFilter,
            @Param("ownerUserIdFilter") Long ownerUserIdFilter,
            @Param("viewerUserId") Long viewerUserId,
            @Param("skipPrivateVisibilityFilter") boolean skipPrivateVisibilityFilter);

    @Query("""
        SELECT t FROM AreaTask t
        WHERE (t.areaId IN :areaIds
            OR t.assignedUserId = :viewerUserId
            OR :viewerUserId MEMBER OF t.assignedUserIds)
          AND (:areaIdFilter IS NULL OR t.areaId = :areaIdFilter)
          AND (
            (:onlyWithoutWorkspace = TRUE AND t.workspaceId IS NULL)
            OR (:onlyWithoutWorkspace = FALSE AND (:workspaceIdFilter IS NULL OR t.workspaceId = :workspaceIdFilter))
          )
          AND (:priorityFilter IS NULL OR t.priority = :priorityFilter)
          AND (:ownerUserIdFilter IS NULL OR t.assignedUserId = :ownerUserIdFilter
              OR :ownerUserIdFilter MEMBER OF t.assignedUserIds)
          AND (:skipPrivateVisibilityFilter = true OR t.privateTask = false
              OR (t.createdByUserId IS NOT NULL AND t.createdByUserId = :viewerUserId))
        ORDER BY t.areaId ASC, t.sortOrder ASC, t.id ASC
        """)
    List<AreaTask> findScopedFiltered(
            @Param("areaIds") Collection<Long> areaIds,
            @Param("areaIdFilter") Long areaIdFilter,
            @Param("workspaceIdFilter") Long workspaceIdFilter,
            @Param("onlyWithoutWorkspace") boolean onlyWithoutWorkspace,
            @Param("priorityFilter") AreaTaskPriority priorityFilter,
            @Param("ownerUserIdFilter") Long ownerUserIdFilter,
            @Param("viewerUserId") Long viewerUserId,
            @Param("skipPrivateVisibilityFilter") boolean skipPrivateVisibilityFilter);

    @Query("""
        SELECT t FROM AreaTask t
        WHERE (:areaIdFilter IS NULL OR t.areaId = :areaIdFilter)
          AND (
            (t.workspaceId IS NULL AND t.createdByUserId = :viewerUserId)
            OR (t.privateTask = true AND t.createdByUserId = :viewerUserId AND t.workspaceId IS NOT NULL)
            OR (t.workspaceId IS NOT NULL
                AND (t.assignedUserId = :viewerUserId OR :viewerUserId MEMBER OF t.assignedUserIds))
          )
          AND (:priorityFilter IS NULL OR t.priority = :priorityFilter)
          AND (:ownerUserIdFilter IS NULL OR t.assignedUserId = :ownerUserIdFilter
              OR :ownerUserIdFilter MEMBER OF t.assignedUserIds)
        ORDER BY t.areaId ASC, t.sortOrder ASC, t.id ASC
        """)
    List<AreaTask> findAdminMySpaceFiltered(
            @Param("areaIdFilter") Long areaIdFilter,
            @Param("priorityFilter") AreaTaskPriority priorityFilter,
            @Param("ownerUserIdFilter") Long ownerUserIdFilter,
            @Param("viewerUserId") Long viewerUserId);

    @Query("""
        SELECT t FROM AreaTask t
        WHERE (t.areaId IN :areaIds
            OR t.assignedUserId = :viewerUserId
            OR :viewerUserId MEMBER OF t.assignedUserIds)
          AND (:areaIdFilter IS NULL OR t.areaId = :areaIdFilter)
          AND (
            (t.workspaceId IS NULL AND t.createdByUserId = :viewerUserId)
            OR (t.privateTask = true AND t.createdByUserId = :viewerUserId AND t.workspaceId IS NOT NULL)
            OR (t.workspaceId IS NOT NULL
                AND (t.assignedUserId = :viewerUserId OR :viewerUserId MEMBER OF t.assignedUserIds)
                AND (t.privateTask = false
                    OR (t.createdByUserId IS NOT NULL AND t.createdByUserId = :viewerUserId)))
          )
          AND (:priorityFilter IS NULL OR t.priority = :priorityFilter)
          AND (:ownerUserIdFilter IS NULL OR t.assignedUserId = :ownerUserIdFilter
              OR :ownerUserIdFilter MEMBER OF t.assignedUserIds)
        ORDER BY t.areaId ASC, t.sortOrder ASC, t.id ASC
        """)
    List<AreaTask> findScopedMySpaceFiltered(
            @Param("areaIds") Collection<Long> areaIds,
            @Param("areaIdFilter") Long areaIdFilter,
            @Param("priorityFilter") AreaTaskPriority priorityFilter,
            @Param("ownerUserIdFilter") Long ownerUserIdFilter,
            @Param("viewerUserId") Long viewerUserId);

    /**
     * Proyectos con al menos una tarea asignada al usuario (incluye tareas cuyo {@code areaId}
     * no pertenece al ámbito del usuario). Misma regla de privadas que el listado scoped.
     */
    @Query("""
        SELECT DISTINCT t.workspaceId FROM AreaTask t
        WHERE t.workspaceId IS NOT NULL
          AND (t.assignedUserId = :viewerUserId OR :viewerUserId MEMBER OF t.assignedUserIds)
          AND (t.privateTask = false
              OR (t.createdByUserId IS NOT NULL AND t.createdByUserId = :viewerUserId))
        """)
    List<Long> findDistinctWorkspaceIdsWhereUserIsAssignee(@Param("viewerUserId") Long viewerUserId);

    long countBySprintId(Long sprintId);

    long countBySprintIdAndStatus(Long sprintId, AreaTaskStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AreaTask t SET t.progressPercent = :pct WHERE t.id = :id")
    int updateProgressPercentById(@Param("id") Long id, @Param("pct") Integer pct);
}
