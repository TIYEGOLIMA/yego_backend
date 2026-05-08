package com.yego.backend.entity.yego_gantt.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveAreaTaskSubtaskDto {

    @NotNull
    private Long targetParentTaskId;
}
