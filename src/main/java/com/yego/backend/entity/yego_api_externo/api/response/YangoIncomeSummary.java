package com.yego.backend.entity.yego_api_externo.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YangoIncomeSummary {

    @JsonProperty("count_completed")
    private Integer countCompleted;

    @JsonProperty("cash_collected")
    private Double cashCollected;

    @JsonProperty("non_cash_payment")
    private Double nonCashPayment;

    @JsonProperty("corporate")
    private Double corporate;

    @JsonProperty("promotion_compensation")
    private Double promotionCompensation;
}
