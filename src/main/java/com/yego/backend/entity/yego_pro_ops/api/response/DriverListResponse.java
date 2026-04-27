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
public class DriverListResponse {

    @JsonProperty("contractors")
    private List<ContractorResponse> contractors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractorResponse {
        @JsonProperty("id")
        private String id;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("phone")
        private String phone;
    }
}
