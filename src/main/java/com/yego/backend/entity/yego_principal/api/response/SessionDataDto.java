package com.yego.backend.entity.yego_principal.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDataDto {
    private List<SessionResponseDto> sessions;
    private SessionStatsDto stats;
    private LocalDateTime generatedAt;
    private String period;
}

