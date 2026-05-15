package com.yego.backend.entity.yego_ticketerera.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta de estadísticas generales de SAC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SacStatsResponse {
    
    private Integer totalSACs;
    private Integer totalTickets;
    private Double averageRating;
    private Integer totalRatings;
    private List<SacPerformanceResponse> sacPerformance;
    private List<SacPerformanceResponse> topPerformers;
    private List<RecentRatingResponse> recentRatings;
    private List<HourlyDistribution> hourlyDistribution;
    private List<HourlyBySede> hourlyBySede;

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
        private Double satisfactionPercentage;
        private String averageResponseTime;
        private List<RatingResponse> ratings;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RatingResponse {
            private Long id;
            private Integer score;
            private String comment;
            private String ticketNumber;
            private String date;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRatingResponse {
        private Long id;
        private String sacName;
        private Integer score;
        private String comment;
        private String ticketNumber;
        private String date;
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
