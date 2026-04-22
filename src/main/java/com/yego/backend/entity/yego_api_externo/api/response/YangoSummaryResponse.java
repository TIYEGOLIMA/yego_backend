package com.yego.backend.entity.yego_api_externo.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YangoSummaryResponse {

    @JsonProperty("resolved_contractor_id")
    private String resolvedContractorId;

    @JsonProperty("weekly")
    private YangoIncomeBlock weekly;

    @JsonProperty("monthly")
    private YangoIncomeBlock monthly;

    @Builder.Default
    @JsonProperty("active_goals")
    private List<JsonNode> activeGoals = new ArrayList<>();

    @Builder.Default
    @JsonProperty("previous_goals")
    private List<JsonNode> previousGoals = new ArrayList<>();

    @JsonProperty("driver")
    private YangoDriverSnapshot driver;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YangoIncomeBlock {
        private YangoPeriod period;

        @JsonProperty("income_summary")
        private YangoIncomeSummary incomeSummary;
    }
}
