package com.yego.backend.entity.yego_api_externo.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YangoSuggestItem {

    @JsonProperty("contractor")
    private YangoSuggestContractor contractor;

    @JsonProperty("vehicle")
    private YangoSuggestVehicle vehicle;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YangoSuggestContractor {
        @JsonProperty("contractor_id")
        private String contractorId;

        @JsonProperty("name")
        private YangoName name;

        @JsonProperty("phone")
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YangoName {
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
    public static class YangoSuggestVehicle {
        @JsonProperty("number")
        private String number;
    }
}
