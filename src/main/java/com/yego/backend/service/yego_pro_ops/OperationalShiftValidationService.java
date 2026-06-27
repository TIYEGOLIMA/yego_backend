package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.OperationalManualComparisonResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationCoverageResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationMismatchResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationalShiftValidationService {

    OperationalValidationSummaryResponse getSummary(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey);

    List<OperationalManualComparisonResponse> getManualComparison(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey,
            Integer limit,
            Integer offset);

    List<OperationalValidationMismatchResponse> getMismatches(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String type,
            Integer limit,
            Integer offset);

    OperationalValidationCoverageResponse getCoverage(
            LocalDateTime from,
            LocalDateTime to,
            String driverId,
            String vehicleKey);
}
