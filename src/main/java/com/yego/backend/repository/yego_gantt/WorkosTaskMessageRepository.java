package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.WorkosTaskMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkosTaskMessageRepository extends JpaRepository<WorkosTaskMessage, Long> {

    String LIST_SELECT_BASE = """
            SELECT m.id AS id,
                   m.task_id AS taskId,
                   m.subtask_id AS subtaskId,
                   m.author_user_id AS authorUserId,
                   CASE WHEN m.author_user_id IS NULL THEN ''
                        ELSE COALESCE(
                            NULLIF(TRIM(BOTH ' ' FROM CONCAT(COALESCE(u.name, ''), ' ', COALESCE(u.last_name, ''))), ''),
                            u.username,
                            '')
                   END AS authorName,
                   m.message_type AS messageType,
                   m.content AS content,
                   m.created_at AS createdAt,
                   m.updated_at AS updatedAt,
                   m.is_deleted AS deleted
            FROM workos_task_messages m
            LEFT JOIN users u ON u.id = m.author_user_id
            """;

    @Query(
            nativeQuery = true,
            value = LIST_SELECT_BASE
                    + " WHERE m.task_id = :taskId AND NOT m.is_deleted ORDER BY m.created_at ASC")
    List<WorkosTaskMessageListRow> findVisibleByTaskWithAuthors(@Param("taskId") Long taskId);

    @Query(
            nativeQuery = true,
            value = LIST_SELECT_BASE
                    + " WHERE m.task_id = :taskId AND m.subtask_id = :subtaskId AND NOT m.is_deleted "
                    + "AND EXISTS (SELECT 1 FROM area_task_subtasks st WHERE st.id = :subtaskId AND st.parent_task_id = :taskId) "
                    + "ORDER BY m.created_at ASC")
    List<WorkosTaskMessageListRow> findVisibleByTaskAndSubtaskWithAuthors(
            @Param("taskId") Long taskId, @Param("subtaskId") Long subtaskId);

    @Query("SELECT m FROM WorkosTaskMessage m WHERE m.id = :id AND m.taskId = :taskId AND NOT m.isDeleted")
    Optional<WorkosTaskMessage> findVisibleByIdAndTaskId(@Param("id") Long id, @Param("taskId") Long taskId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM WorkosTaskMessage m WHERE m.taskId = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);

    /** Reasigna mensajes de hilo de subtarea al nuevo padre (misma fila en area_task_subtasks). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    "UPDATE workos_task_messages SET task_id = :newTaskId, updated_at = NOW() "
                            + "WHERE subtask_id = :subtaskId AND task_id = :oldTaskId AND is_deleted = FALSE",
            nativeQuery = true)
    int reassignSubtaskThreadToParent(
            @Param("oldTaskId") long oldTaskId,
            @Param("subtaskId") long subtaskId,
            @Param("newTaskId") long newTaskId);

    /** Reasigna mensajes de hilo de una tarea a su nueva entidad subtarea. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    "UPDATE workos_task_messages SET task_id = :newParentTaskId, subtask_id = :newSubtaskId, updated_at = NOW() "
                            + "WHERE task_id = :oldTaskId",
            nativeQuery = true)
    int reassignTaskThreadToSubtask(
            @Param("oldTaskId") long oldTaskId,
            @Param("newParentTaskId") long newParentTaskId,
            @Param("newSubtaskId") long newSubtaskId);
}
