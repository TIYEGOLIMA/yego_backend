package com.yego.backend.entity.yego_api_externo.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YangoDriverSnapshot {

    @JsonProperty("driver_id")
    private String driverId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("license_number")
    private String licenseNumber;

    @JsonProperty("balance")
    private Double balance;

    @JsonProperty("balance_limit")
    private Double balanceLimit;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("average_rating")
    private Double averageRating;
}
