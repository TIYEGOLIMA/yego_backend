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

    /** Total neto en balances según API de ingresos Yango ({@code balances.total}). */
    @JsonProperty("total")
    private Double total;

    @JsonProperty("cash_collected")
    private Double cashCollected;

    @JsonProperty("non_cash_payment")
    private Double nonCashPayment;

    @JsonProperty("corporate")
    private Double corporate;

    /** Compensación por promoción cliente/descuentos ({@code balances.platform_promotion}). */
    @JsonProperty("promotion_compensation")
    private Double promotionCompensation;

    /** Bonificación de plataforma ({@code balances.platform_bonus}). */
    @JsonProperty("bonificacion")
    private Double bonificacion;

    /** Propinas agregadas según {@code balances} de la API de ingresos (tip / platform_tip / partner_ride_tip). */
    @JsonProperty("tips")
    private Double tips;

    // ── Desglose de cargos en balances Yango (suelen venir como valores negativos: comisiones, impuestos, otros). ──

    /** Comisión / cargos por servicio de la plataforma ({@code platform_fees}). */
    @JsonProperty("platform_fees")
    private Double platformFees;

    /** Comisión de la empresa asociada ({@code partner_fees}). */
    @JsonProperty("partner_fees")
    private Double partnerFees;

    /** Pagos relacionados con combustible en plataforma ({@code platform_gas}). */
    @JsonProperty("platform_gas")
    private Double platformGas;

    /** Otros pagos cargados por la plataforma ({@code platform_other}). */
    @JsonProperty("platform_other")
    private Double platformOther;

    /** Impuestos u obligaciones fiscales asociadas al servicio ({@code mandatory_taxes_fee}). */
    @JsonProperty("mandatory_taxes_fee")
    private Double mandatoryTaxesFee;

    /** Otros conceptos de marketing vía plataforma ({@code platform_marketing_other}). */
    @JsonProperty("platform_marketing_other")
    private Double platformMarketingOther;

    /** Otros pagos del lado del socio / empresa asociada ({@code partner_contractor_other}). */
    @JsonProperty("partner_contractor_other")
    private Double partnerContractorOther;

    /**
     * Efectivo + pago sin efectivo + corporativo + propinas + compensación por promoción + bonificación
     * (Yego Pro / Yango Pro).
     */
    @JsonProperty("price_yango_pro")
    private Double priceYangoPro;
}
