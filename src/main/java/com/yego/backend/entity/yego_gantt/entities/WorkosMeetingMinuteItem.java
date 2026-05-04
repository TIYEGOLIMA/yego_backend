package com.yego.backend.entity.yego_gantt.entities;

import com.yego.backend.entity.yego_gantt.entities.enums.WorkosMeetingItemStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.WorkosMeetingItemType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workos_meeting_minute_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_wmmi_minute_order", columnNames = {"meeting_minute_id", "item_order"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkosMeetingMinuteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_minute_id", nullable = false)
    private WorkosMeetingMinute meetingMinute;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "area_name_snapshot", length = 255)
    private String areaNameSnapshot;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    @Builder.Default
    private WorkosMeetingItemType itemType = WorkosMeetingItemType.ACCION;

    @Column(columnDefinition = "TEXT")
    private String situation;

    @Column(columnDefinition = "TEXT")
    private String decision;

    @Column(name = "task_title", length = 255)
    private String taskTitle;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @Column(name = "responsible_name_snapshot", length = 255)
    private String responsibleNameSnapshot;

    @Column(name = "start_date")
    private LocalDate startDate;

    private LocalDate deadline;

    @Column(length = 50)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private WorkosMeetingItemStatus status = WorkosMeetingItemStatus.PENDIENTE;

    @Column(name = "converted_task_id")
    private Long convertedTaskId;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "converted_by_user_id")
    private Long convertedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
        if (itemType == null) {
            itemType = WorkosMeetingItemType.ACCION;
        }
        if (status == null) {
            status = WorkosMeetingItemStatus.PENDIENTE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
