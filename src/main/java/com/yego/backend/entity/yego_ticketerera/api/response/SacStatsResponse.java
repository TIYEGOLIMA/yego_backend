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
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SacPerformanceResponse {
        private Long id;
        private String name;
        private String username;
        private Integer totalTickets;
        private Integer completedTickets;
        private Double averageRating;
        private Integer totalRatings;
        private Double satisfactionPercentage;
        private String averageResponseTime;
        private String lastActivity;
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
}
