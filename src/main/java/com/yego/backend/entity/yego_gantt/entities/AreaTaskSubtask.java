package com.yego.backend.entity.yego_gantt.entities;

import com.yego.backend.entity.yego_gantt.entities.enums.AreaTaskStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "area_task_subtasks", indexes = {
        @Index(name = "idx_area_task_subtasks_parent_id", columnList = "parent_task_id"),
        @Index(name = "idx_area_task_subtasks_parent_sort", columnList = "parent_task_id,sort_order"),
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTaskSubtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_task_id", nullable = false)
    private Long parentTaskId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 4000)
    private String description;

    /** Objetivos / resultado esperado de la subtarea (texto libre). */
    @Column(length = 4000)
    private String objectives;

    /** Lista de checklist serializada como JSON (array de { id?, text, done }). */
    @Column(name = "checklist_json", columnDefinition = "TEXT")
    private String checklistJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean done = false;

    /**
     * Columna/agrupación Kanban por subtarea, independiente del estado del proyecto padre.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kanban_status", nullable = false, length = 32)
    @Builder.Default
    private AreaTaskStatus kanbanStatus = AreaTaskStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Nullable: si es null, UI/API muestran el área del padre. */
    @Column(name = "area_id")
    private Long areaId;

    /** Nullable: si es null, UI/API muestran el {@code workspaceId / project_id} del padre. */
    @Column(name = "project_id")
    private Long workspaceId;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (weight == null) weight = BigDecimal.ONE;
        if (sortOrder == null) sortOrder = 0;
        if (done == null) done = false;
        if (kanbanStatus == null) {
            kanbanStatus = AreaTaskStatus.PENDING;
        }
        if (Boolean.TRUE.equals(done) && kanbanStatus != AreaTaskStatus.DONE) {
            kanbanStatus = AreaTaskStatus.DONE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (weight == null) weight = BigDecimal.ONE;
        if (sortOrder == null) sortOrder = 0;
        if (done == null) done = false;
        if (kanbanStatus == null) {
            kanbanStatus = AreaTaskStatus.PENDING;
        }
        if (kanbanStatus == AreaTaskStatus.DONE) {
            done = true;
        }
        if (Boolean.TRUE.equals(done) && kanbanStatus != AreaTaskStatus.DONE) {
            kanbanStatus = AreaTaskStatus.DONE;
        }
    }
}
