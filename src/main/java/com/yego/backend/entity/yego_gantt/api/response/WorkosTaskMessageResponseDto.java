package com.yego.backend.entity.yego_gantt.api.response;

import com.yego.backend.entity.yego_gantt.entities.enums.WorkosTaskMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkosTaskMessageResponseDto {

    private Long id;
    private Long taskId;
    private Long subtaskId;
    private Long authorUserId;
    private String authorName;
    private WorkosTaskMessageType messageType;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}
