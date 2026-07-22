package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Contrato del reporte operativo de Ticketera.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SacStatsResponse {
    
    private Integer totalTickets;
    private Double averageRating;
    private Integer totalRatings;
    private Integer openTickets;
    private Integer completedTickets;
    private Integer cancelledTickets;
    private Integer traceabilityTotal;
    private List<SacPerformanceResponse> sacPerformance;
    private List<HourlyDistribution> hourlyDistribution;
    private List<HourlyBySede> hourlyBySede;
    private List<OptionSelectionBySedeResponse> optionSelectionsBySede;
    private List<TicketTraceabilityResponse> ticketTraceability;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionSelectionBySedeResponse {
        private Long sedeId;
        private String sedeName;
        private Integer totalTickets;
        private List<OptionSelectionResponse> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionSelectionResponse {
        private Long optionId;
        private String categoryName;
        private String optionName;
        private Integer count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTraceabilityResponse {
        private Long id;
        private String ticketNumber;
        private String status;
        private Long sedeId;
        private String sedeName;
        private Long optionId;
        private String categoryName;
        private String optionName;
        private String licenseNumber;
        private Long moduleId;
        private String moduleName;
        private Long operatorId;
        private String operatorName;
        private LocalDateTime createdAt;
        private LocalDateTime calledAt;
        private LocalDateTime completedAt;
        private Integer rating;
        private List<TicketTraceEventResponse> events;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTraceEventResponse {
        private String status;
        private String label;
        private LocalDateTime occurredAt;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyBySede {
        private Long sedeId;
        private String sedeName;
        private List<HourlyDistribution> hourlyDistribution;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SacPerformanceResponse {
        private Long id;
        private String name;
        private String username;
        private Long sedeId;
        private String sedeName;
        private Integer totalTickets;
        private Integer completedTickets;
        private Double averageRating;
        private Integer totalRatings;
        private Double resolutionPercentage;
        private String averageServiceTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyDistribution {
        private int hour;
        private String label;
        private long count;
    }
}
