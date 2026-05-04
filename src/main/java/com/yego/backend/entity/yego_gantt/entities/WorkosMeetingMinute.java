package com.yego.backend.entity.yego_gantt.entities;

import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workos_meeting_minutes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkosMeetingMinute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", length = 50)
    private MeetingMinuteType meetingType;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MeetingMinuteStatus status = MeetingMinuteStatus.ABIERTA;

    @Column(name = "next_meeting_date")
    private LocalDate nextMeetingDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "meetingMinute", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkosMeetingMinuteItem> items = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
        if (status == null) {
            status = MeetingMinuteStatus.ABIERTA;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
