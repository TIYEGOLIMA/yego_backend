package com.yego.backend.entity.yego_gantt.entities;

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

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean done = false;

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

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (weight == null) weight = BigDecimal.ONE;
        if (sortOrder == null) sortOrder = 0;
        if (done == null) done = false;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
