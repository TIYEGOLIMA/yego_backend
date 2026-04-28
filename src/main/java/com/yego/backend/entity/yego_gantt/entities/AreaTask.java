package com.yego.backend.entity.yego_gantt.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "area_tasks", indexes = {
        @Index(name = "idx_area_tasks_area_id", columnList = "area_id"),
        @Index(name = "idx_area_tasks_project_id", columnList = "project_id"),
        @Index(name = "idx_area_tasks_sprint_id", columnList = "sprint_id"),
        @Index(name = "idx_area_tasks_area_priority", columnList = "area_id,priority"),
})
@BatchSize(size = 32)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private AreaTaskStatus status = AreaTaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    @Builder.Default
    private AreaTaskPriority priority = AreaTaskPriority.MEDIUM;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "area_task_assignees", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "user_id")
    @Builder.Default
    private List<Long> assignedUserIds = new ArrayList<>();

    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "area_task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag", length = 200)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (progressPercent == null) progressPercent = 0;
        if (sortOrder == null) sortOrder = 0;
        if (status == null) status = AreaTaskStatus.PENDING;
        if (priority == null) priority = AreaTaskPriority.MEDIUM;
        if (assignedUserIds == null) assignedUserIds = new ArrayList<>();
        if (tags == null) tags = new ArrayList<>();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
