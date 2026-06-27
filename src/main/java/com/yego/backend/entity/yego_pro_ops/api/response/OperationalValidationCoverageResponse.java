package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalValidationCoverageResponse {
    private long operationalTripFactCount;
    private long tripFactsWithDriverIdCount;
    private long tripFactsWithVehicleKeyCount;
    private long tripFactsWithNormalizedPlateCount;
    private long tripFactsMissingVehicleCount;
    private long operationalShiftCount;
    private long highConfidenceShiftCount;
    private long mediumConfidenceShiftCount;
    private long lowConfidenceShiftCount;
    private long needsReviewShiftCount;
    private double vehicleKeyCoveragePct;
    private double needsReviewShiftPct;
}
