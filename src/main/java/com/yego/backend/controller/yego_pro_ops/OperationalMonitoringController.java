package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftEventResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalShiftSessionResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OperationalTripFactResponse;
import com.yego.backend.service.yego_pro_ops.OperationalShiftInferenceService;
import com.yego.backend.service.yego_pro_ops.OperationalTripFactService;
import com.yego.backend.service.yego_pro_ops.impl.OperationalDateRangeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pro-ops/operational-monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OperationalMonitoringController {

    private final OperationalTripFactService operationalTripFactService;
    private final OperationalShiftInferenceService operationalShiftInferenceService;
    private final OperationalDateRangeParser dateRangeParser;

    @GetMapping("/trip-facts")
    public List<OperationalTripFactResponse> getTripFacts(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String vehicleKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        LocalDateTime parsedFrom = dateRangeParser.parseFrom(from);
        LocalDateTime parsedTo = dateRangeParser.parseTo(to);
        return operationalTripFactService.searchTripFacts(parsedFrom, parsedTo, driverId, vehicleKey, status, limit);
    }

    @GetMapping("/shifts")
    public List<OperationalShiftSessionResponse> getShifts(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String vehicleKey,
            @RequestParam(required = false) String state) {
        return operationalShiftInferenceService.searchShifts(
                dateRangeParser.parseFrom(from),
                dateRangeParser.parseTo(to),
                driverId,
                vehicleKey,
                state);
    }

    @GetMapping("/events")
    public List<OperationalShiftEventResponse> getEvents(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String vehicleKey) {
        return operationalShiftInferenceService.searchEvents(
                dateRangeParser.parseFrom(from),
                dateRangeParser.parseTo(to),
                shiftId,
                driverId,
                vehicleKey);
    }
}
