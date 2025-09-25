package com.yego.backend.entity.yego_principal.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas semanales YEGO Principal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsDto {
    private Long newUsers;
    private Long imports;
    private Long activity;
    private String period;
}
