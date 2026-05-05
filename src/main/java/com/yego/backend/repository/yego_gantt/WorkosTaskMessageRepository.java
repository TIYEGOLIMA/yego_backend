package com.yego.backend.repository.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.WorkosTaskMessage;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * Hilo por subtarea: una sola consulta (sin EXISTS previo en servicio).
     * Si {@code subtaskId} no pertenece a {@code taskId}, el EXISTS devuelve 0 filas.
     */
    @Query(
            nativeQuery = true,
            value = LIST_SELECT_BASE
                    + " WHERE m.task_id = :taskId AND m.subtask_id = :subtaskId AND NOT m.is_deleted "
                    + "AND EXISTS (SELECT 1 FROM area_task_subtasks st WHERE st.id = :subtaskId AND st.parent_task_id = :taskId) "
                    + "ORDER BY m.created_at ASC")
    List<WorkosTaskMessageListRow> findVisibleByTaskAndSubtaskWithAuthors(
            @Param("taskId") Long taskId, @Param("subtaskId") Long subtaskId);

    @Query("SELECT m FROM WorkosTaskMessage m WHERE m.taskId = :taskId AND NOT m.isDeleted ORDER BY m.createdAt ASC")
    List<WorkosTaskMessage> findVisibleByTaskIdOrderByCreatedAtAsc(@Param("taskId") Long taskId);

    @Query("SELECT m FROM WorkosTaskMessage m WHERE m.taskId = :taskId AND m.subtaskId = :subtaskId AND NOT m.isDeleted ORDER BY m.createdAt ASC")
    List<WorkosTaskMessage> findVisibleByTaskIdAndSubtaskIdOrderByCreatedAtAsc(
            @Param("taskId") Long taskId, @Param("subtaskId") Long subtaskId);

    @Query("SELECT m FROM WorkosTaskMessage m WHERE m.id = :id AND m.taskId = :taskId AND NOT m.isDeleted")
    Optional<WorkosTaskMessage> findVisibleByIdAndTaskId(@Param("id") Long id, @Param("taskId") Long taskId);
}
