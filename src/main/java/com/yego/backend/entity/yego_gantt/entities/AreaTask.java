package com.yego.backend.entity.yego_gantt.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "area_tasks")
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

    @Column(name = "assigned_user_ids", length = 1000)
    private String assignedUserIds;

    @Column(name = "tags", length = 2000)
    private String tags;

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
        if (progressPercent == null) {
            progressPercent = 0;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (status == null) {
            status = AreaTaskStatus.PENDING;
        }
        if (priority == null) {
            priority = AreaTaskPriority.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
