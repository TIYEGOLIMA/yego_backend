package com.yego.backend.entity.yego_gantt.api.response;

import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMinuteResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryKpis {
        private int totalItems;
        private int convertedItems;
        private int unconvertedItems;
        private int completedTasks;
        private int inProgressTasks;
        private int blockedTasks;
        private int overdueTasks;
        private int pendingWithoutTask;
        private double completionPercentage;
    }

    private Long id;
    private String title;
    private LocalDate meetingDate;
    private MeetingMinuteType meetingType;
    private String summary;
    private Long createdByUserId;
    private Long ownerUserId;
    private MeetingMinuteStatus status;
    private LocalDate nextMeetingDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MeetingMinuteItemResponse> items;
    private SummaryKpis kpis;
    /** true si el usuario no es admin/creador/dueño: solo recibe ítems donde es responsable o el ítem es de su área. */
    @Builder.Default
    private boolean partialItemsView = false;
}
