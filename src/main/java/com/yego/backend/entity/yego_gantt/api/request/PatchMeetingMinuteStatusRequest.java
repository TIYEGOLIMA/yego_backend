package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchMeetingMinuteStatusRequest {

    @NotNull
    private MeetingMinuteStatus status;
}
