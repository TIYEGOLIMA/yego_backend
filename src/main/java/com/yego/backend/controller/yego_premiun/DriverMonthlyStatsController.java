package com.yego.backend.controller.yego_premiun;

import com.yego.backend.entity.yego_premiun.api.request.DriverMonthlyStatsProcessRequest;
import com.yego.backend.entity.yego_premiun.api.response.DriverMonthlyStatsResponse;
import com.yego.backend.entity.yego_premiun.api.response.DriverTripsMonthResponse;
import com.yego.backend.entity.yego_premiun.api.response.DriverTripsYearResponse;
import com.yego.backend.service.yego_premiun.DriverMonthlyStatsService;
import com.yego.backend.service.yego_premiun.DriverTripsMonthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/yego-premiun")
@RequiredArgsConstructor
@Slf4j
public class DriverMonthlyStatsController {

    private final DriverMonthlyStatsService driverMonthlyStatsService;
    private final DriverTripsMonthService driverTripsMonthService;

    @PostMapping("/driver-active/process")
    public ResponseEntity<List<DriverMonthlyStatsResponse>> procesarActivos(@Valid @RequestBody DriverMonthlyStatsProcessRequest request) {
        log.info("[DriverMonthlyStatsController] POST process month={} year={}", request.getMonth(), request.getYear());
        List<DriverMonthlyStatsResponse> response = driverMonthlyStatsService.procesarYListarActivos(request.getMonth(), request.getYear());
        log.debug("[DriverMonthlyStatsController] process result size={}", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver-active/list")
    public ResponseEntity<List<DriverMonthlyStatsResponse>> listarActivos() {
        List<DriverMonthlyStatsResponse> response = driverMonthlyStatsService.listarActivos();
        log.debug("[DriverMonthlyStatsController] GET list size={}", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Viajes completados en {@code trips_{year}} para un conductor en un mes (misma ventana que el procesamiento premiun).
     */
    @GetMapping("/driver-trips/month")
    public ResponseEntity<DriverTripsMonthResponse> viajesCompletadosPorMes(
            @RequestParam String driverId,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            DriverTripsMonthResponse response =
                    driverTripsMonthService.listCompletedTripsForMonth(driverId, month, year);
            log.debug(
                    "[DriverMonthlyStatsController] GET driver-trips/month driverId={} month={} year={} count={}",
                    driverId, month, year, response.getCompletedTripsCount());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[DriverMonthlyStatsController] driver-trips/month invalid: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Viajes completados y Yango Pro por mes en todo el año (tabla {@code trips_{year}}).
     */
    @GetMapping("/driver-trips/year")
    public ResponseEntity<DriverTripsYearResponse> viajesCompletadosPorAnio(
            @RequestParam String driverId,
            @RequestParam int year) {
        try {
            DriverTripsYearResponse response = driverTripsMonthService.listCompletedTripsForYear(driverId, year);
            log.debug(
                    "[DriverMonthlyStatsController] GET driver-trips/year driverId={} year={} totalTrips={}",
                    driverId, year, response.getTotalCompletedTrips());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[DriverMonthlyStatsController] driver-trips/year invalid: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

