package com.yego.backend.entity.yego_gantt.api.request;

import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingMinuteRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotNull
    private LocalDate meetingDate;

    private MeetingMinuteType meetingType;

    private String summary;

    private Long ownerUserId;

    private LocalDate nextMeetingDate;

    private MeetingMinuteStatus status;
}
