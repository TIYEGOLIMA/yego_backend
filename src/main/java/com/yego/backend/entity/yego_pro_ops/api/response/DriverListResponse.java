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

        @JsonProperty("lead_id")
        private String leadId;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        @JsonProperty("balance")
        private String balance;

        @JsonProperty("balance_limit")
        private String balanceLimit;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("groups")
        private List<String> groups;

        @JsonProperty("hiring_segment")
        private String hiringSegment;

        @JsonProperty("last_order_date")
        private String lastOrderDate;

        @JsonProperty("lifecycle_step")
        private String lifecycleStep;

        @JsonProperty("name")
        private NameResponse name;

        @JsonProperty("orders_count")
        private Integer ordersCount;

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("status")
        private String status;

        @JsonProperty("violations")
        private List<String> violations;

        @JsonProperty("attestation_issues")
        private List<String> attestationIssues;

        @JsonProperty("unblock_date")
        private String unblockDate;

        @JsonProperty("photocheck_restrictions")
        private List<String> photocheckRestrictions;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NameResponse {
            @JsonProperty("first")
            private String first;

            @JsonProperty("last")
            private String last;

            @JsonProperty("middle")
            private String middle;
        }
    }
}

