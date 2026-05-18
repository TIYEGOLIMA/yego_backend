package com.yego.backend.entity.yego_pro_ops.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yego.backend.entity.yego_pro_ops.entities.BonusThreshold;
import com.yego.backend.entity.yego_pro_ops.entities.PaymentPercentage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingConfigResponse {

    @JsonProperty("bonus_thresholds")
    private List<BonusThreshold> bonusThresholds;

    @JsonProperty("payment_percentages")
    private List<PaymentPercentage> paymentPercentages;
}
