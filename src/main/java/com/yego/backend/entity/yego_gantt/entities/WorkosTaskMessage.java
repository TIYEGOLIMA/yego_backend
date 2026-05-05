package com.yego.backend.entity.yego_gantt.entities;

import com.yego.backend.entity.yego_gantt.entities.enums.WorkosTaskMessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "workos_task_messages",
        indexes = {
                @Index(name = "idx_workos_task_messages_task_id_created_at", columnList = "task_id,created_at"),
                @Index(name = "idx_workos_task_messages_author_user_id", columnList = "author_user_id"),
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkosTaskMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "subtask_id")
    private Long subtaskId;

    @Column(name = "author_user_id")
    private Long authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private WorkosTaskMessageType messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
