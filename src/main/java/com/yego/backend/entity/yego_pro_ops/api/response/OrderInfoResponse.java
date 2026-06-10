package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfoResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("short_id")
    private Long shortId;

    @JsonProperty("id")
    private String id;

    @JsonProperty("ended_at")
    private String endedAt;

    @JsonProperty("booked_at")
    private String bookedAt;

    @JsonProperty("car_brand_model")
    private String carBrandModel;

    @JsonProperty("distance")
    private Double distance;

    @JsonProperty("price_cash")
    private Double cash;

    @JsonProperty("card")
    private Double card;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("price_bonus")
    private Double priceBonus;

    @JsonProperty("price_commission_park")
    private Double priceCommissionPark;

    @JsonProperty("price_commission_service")
    private Double priceCommissionService;

    @JsonProperty("price_corporate")
    private Double priceCorporate;

    @JsonProperty("price_other")
    private Double priceOther;

    @JsonProperty("price_promotion")
    private Double pricePromotion;

    @JsonProperty("price_tip")
    private Double priceTip;

    @JsonProperty("car_license_number")
    private String carLicenseNumber;

    @JsonProperty("address_from")
    private String addressFrom;

    @JsonProperty("address_to")
    private String addressTo;
}
