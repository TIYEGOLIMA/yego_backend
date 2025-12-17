package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkRulesResponse {

    @JsonProperty("work_rules")
    private List<WorkRule> workRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkRule {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }
}

