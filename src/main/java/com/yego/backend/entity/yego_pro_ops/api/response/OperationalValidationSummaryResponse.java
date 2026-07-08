package com.yego.backend.entity.yego_pro_ops.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalValidationSummaryResponse {
    private long operationalShiftCount;
    private long manualShiftCount;
    private long matchedShiftCount;
    private long unmatchedOperationalShiftCount;
    private long unmatchedManualShiftCount;
    private long operationalTripFactCount;
    private long tripFactsWithVehicleKeyCount;
    private long tripFactsMissingVehicleKeyCount;
    private double vehicleKeyCoveragePct;
    private long needsReviewShiftCount;
    private double needsReviewShiftPct;
    private long autoClosedByNextDriverCount;
    private long staleCandidateCount;
    private Double averageStartDeltaMinutes;
    private Double averageEndDeltaMinutes;
    private Double p50StartDeltaMinutes;
    private Double p95StartDeltaMinutes;
    private Double p50EndDeltaMinutes;
    private Double p95EndDeltaMinutes;
    private String manualReplacementReadiness;
    private String readinessReason;
}
