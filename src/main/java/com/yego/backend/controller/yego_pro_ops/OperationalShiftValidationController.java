package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.OperationalManualComparisonResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationCoverageResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationMismatchResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalValidationSummaryResponse;
import com.yego.backend.service.yego_pro_ops.OperationalShiftValidationService;
import com.yego.backend.service.yego_pro_ops.impl.OperationalDateRangeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pro-ops/operational-monitoring/validation")
@RequiredArgsConstructor
public class OperationalShiftValidationController {

    private final OperationalShiftValidationService validationService;
    private final OperationalDateRangeParser dateRangeParser;

    @GetMapping("/summary")
    public OperationalValidationSummaryResponse getSummary(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String vehicleKey) {
        LocalDateTime parsedFrom = requireValidRangeFrom(from);
        LocalDateTime parsedTo = requireValidRangeTo(parsedFrom, to);
        return validationService.getSummary(parsedFrom, parsedTo, driverId, vehicleKey);
    }

    @GetMapping("/manual-comparison")
    public List<OperationalManualComparisonResponse> getManualComparison(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String vehicleKey,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        LocalDateTime parsedFrom = requireValidRangeFrom(from);
        LocalDateTime parsedTo = requireValidRangeTo(parsedFrom, to);
        return validationService.getManualComparison(parsedFrom, parsedTo, driverId, vehicleKey, limit, offset);
    }

    @GetMapping("/mismatches")
    public List<OperationalValidationMismatchResponse> getMismatches(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        LocalDateTime parsedFrom = requireValidRangeFrom(from);
        LocalDateTime parsedTo = requireValidRangeTo(parsedFrom, to);
        return validationService.getMismatches(parsedFrom, parsedTo, driverId, type, limit, offset);
    }

    @GetMapping("/coverage")
    public OperationalValidationCoverageResponse getCoverage(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String vehicleKey) {
        LocalDateTime parsedFrom = requireValidRangeFrom(from);
        LocalDateTime parsedTo = requireValidRangeTo(parsedFrom, to);
        return validationService.getCoverage(parsedFrom, parsedTo, driverId, vehicleKey);
    }

    private LocalDateTime requireValidRangeFrom(String from) {
        if (from == null || from.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`from` is required");
        }
        return dateRangeParser.parseFrom(from);
    }

    private LocalDateTime requireValidRangeTo(LocalDateTime parsedFrom, String to) {
        if (to == null || to.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`to` is required");
        }
        LocalDateTime parsedTo = dateRangeParser.parseTo(to);
        if (parsedTo.isBefore(parsedFrom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`to` must be greater than or equal to `from`");
        }
        return parsedTo;
    }
}
