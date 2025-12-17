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
    
    @JsonProperty("driver_id")
    private String driverId;
    
    @JsonProperty("driver_full_name")
    private String driverFullName;
    
    @JsonProperty("ended_at")
    private String endedAt;
    
    @JsonProperty("booked_at")
    private String bookedAt;
    
    @JsonProperty("car_brand_model")
    private String carBrandModel;
    
    @JsonProperty("distance")
    private Double distance; // Distancia en kilómetros
    
    @JsonProperty("cash")
    private Double cash; // Pago en efectivo
    
    @JsonProperty("card")
    private Double card; // Pago con tarjeta
    
    @JsonProperty("price")
    private Double price; // Precio en Yango Pro
    
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
    
    @JsonProperty("price_partner_rides")
    private Double pricePartnerRides;
    
    @JsonProperty("price_promotion")
    private Double pricePromotion;
    
    @JsonProperty("price_tip")
    private Double priceTip;
}

