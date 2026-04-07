package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractorSuggestionsResponse {

    @JsonProperty("suggestions")
    private List<ContractorSuggestionItem> suggestions;

    @JsonProperty("existing")
    private boolean existing;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContractorSuggestionItem {
        @JsonProperty("contractor")
        private ContractorSuggestionContractor contractor;

        @JsonProperty("vehicle")
        private ContractorSuggestionVehicle vehicle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContractorSuggestionVehicle {
        @JsonProperty("number")
        private String number;

        @JsonProperty("brand")
        private String brand;

        @JsonProperty("model")
        private String model;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContractorSuggestionContractor {
        @JsonProperty("name")
        private NamePart name;

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("segment")
        private SegmentPart segment;

        @JsonProperty("employment_type")
        private String employmentType;

        @JsonProperty("profession_name")
        private String professionName;

        @JsonProperty("contractor_id")
        private String contractorId;

        @JsonProperty("lead_id")
        private String leadId;

        @JsonProperty("balance")
        private String balance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NamePart {
        @JsonProperty("first")
        private String first;

        @JsonProperty("last")
        private String last;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentPart {
        @JsonProperty("segment")
        private String segment;
    }
}
