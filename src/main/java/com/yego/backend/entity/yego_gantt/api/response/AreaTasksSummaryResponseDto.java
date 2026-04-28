package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTasksSummaryResponseDto {
    private List<AreaTaskResponseDto> tasks;
    private AreaTaskKpisResponseDto kpis;
}
