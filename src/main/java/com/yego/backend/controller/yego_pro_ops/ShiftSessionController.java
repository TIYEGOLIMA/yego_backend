package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.CloseShiftSessionRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.SettleShiftSessionRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.ShiftSessionResponse;
import com.yego.backend.service.yego_pro_ops.DriverOrdersService;
import com.yego.backend.service.yego_pro_ops.ShiftSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/pro-ops/shift-sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShiftSessionController {

    private static final DateTimeFormatter DATETIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATETIME_MINUTES_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ShiftSessionService shiftSessionService;
    private final DriverOrdersService driverOrdersService;

    @GetMapping("/active/{driverId}")
    public ShiftSessionResponse getActiveSession(@PathVariable String driverId) {
        ShiftSessionResponse session = shiftSessionService.getActiveSession(driverId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay sesión activa para el conductor: " + driverId);
        }
        return session;
    }

    @GetMapping("/driver/{driverId}")
    public List<ShiftSessionResponse> getDriverSessionHistory(@PathVariable String driverId) {
        return shiftSessionService.getDriverSessionHistory(driverId);
    }

    @GetMapping("/closed")
    public List<ShiftSessionResponse> getClosedSessionsForExternalConsult(
            @RequestParam(required = false) String driverId,
            @RequestParam String desde,
            @RequestParam String hasta) {
        LocalDateTime desdeParsed = parseDateTime(desde);
        LocalDateTime hastaParsed = parseDateTime(hasta);
        if (hastaParsed.isBefore(desdeParsed)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hasta no puede ser menor que desde");
        }
        return shiftSessionService.getClosedSessionsForExternalConsult(driverId, desdeParsed, hastaParsed);
    }

    @GetMapping("/closed/by-date")
    public List<ShiftSessionResponse> getClosedSessionsByDateForExternalConsult(
            @RequestParam(name = "driver_id") String driverId,
            @RequestParam String fecha) {
        LocalDateTime desde = parseDate(fecha).atStartOfDay();
        LocalDateTime hasta = parseDate(fecha).atTime(23, 59, 59);
        return shiftSessionService.getClosedSessionsForExternalConsult(driverId, desde, hasta);
    }

    @GetMapping("/yango-trips/by-date")
    public List<OrderInfoResponse> getYangoTripsByDateForExternalConsult(
            @RequestParam(name = "driver_id") String driverId,
            @RequestParam String fecha) {
        java.time.LocalDate date = parseDate(fecha);
        String desde = date.atStartOfDay().format(DATETIME_SECONDS_FORMATTER) + "-05:00";
        String hasta = date.atTime(23, 59, 59).format(DATETIME_SECONDS_FORMATTER) + "-05:00";
        var response = driverOrdersService.obtenerViajesCompletos(driverId, desde, hasta);
        return response != null && response.getOrders() != null ? response.getOrders() : List.of();
    }

    @PostMapping("/{sessionId}/close")
    public ShiftSessionResponse closeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CloseShiftSessionRequest request) {
        log.info("[ShiftSessionController] cerrar sesión sessionId={} closedBy={}", sessionId, request.getClosedBy());
        try {
            return shiftSessionService.closeSession(sessionId, request.getClosedBy());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/settle")
    public ShiftSessionResponse settleSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SettleShiftSessionRequest request) {
        log.info("[ShiftSessionController] liquidar sesión sessionId={} settledBy={}", sessionId, request.getSettledBy());
        try {
            return shiftSessionService.settleSession(sessionId, request.getSettledBy());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{sessionId}")
    public void eliminarSesion(
            @PathVariable UUID sessionId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {
        log.info("[ShiftSessionController] eliminar sesión sessionId={} userId={}", sessionId, userId);
        try {
            shiftSessionService.eliminarSesion(sessionId, userId, reason);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/status")
    public ShiftSessionResponse updateStatus(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'status' es requerido");
        }
        log.info("[ShiftSessionController] actualizar estado sessionId={} status={}", sessionId, status);
        try {
            return shiftSessionService.updateSessionStatus(sessionId, status);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato requerido: yyyy-MM-ddTHH:mm o yyyy-MM-ddTHH:mm:ss");
        }
        try {
            return LocalDateTime.parse(value, DATETIME_SECONDS_FORMATTER);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value, DATETIME_MINUTES_FORMATTER);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Formato requerido: yyyy-MM-ddTHH:mm o yyyy-MM-ddTHH:mm:ss");
            }
        }
    }

    private java.time.LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato requerido: yyyy-MM-dd");
        }
        try {
            return java.time.LocalDate.parse(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato requerido: yyyy-MM-dd");
        }
    }
}
