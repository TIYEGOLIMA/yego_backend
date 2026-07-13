package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.mobile.OpenShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.MobileShiftLocationRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.ShiftLocationResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftSummaryResponse;
import com.yego.backend.service.yego_pro_ops.mobile.MobileDriverAuthService;
import com.yego.backend.service.yego_pro_ops.mobile.MobileShiftLocationService;
import com.yego.backend.service.yego_pro_ops.mobile.MobileShiftService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de turnos para la app móvil del conductor.
 * Permite abrir/cerrar turnos, consultar resumen Yango y ver historial.
 */
@Slf4j
@RestController
@RequestMapping("/api/mobile/shifts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MobileShiftController {

    private final MobileShiftService service;
    private final MobileShiftLocationService locationService;
    private final MobileDriverAuthService mobileDriverAuthService;

    /** Abrir turno desde la app móvil */
    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MobileShiftResponse> openShift(
            @Valid @RequestBody OpenShiftMobileRequest request,
            HttpServletRequest httpRequest
    ) {
        String driverId = mobileDriverAuthService.requireDriverId(httpRequest);
        log.info("Apertura de turno: driver={}, placa={}", driverId, request.getPlaca());
        MobileShiftResponse response = service.openShift(driverId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Obtener resumen Yango (viajes, producido, efectivo) ANTES de confirmar cierre */
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<MobileShiftSummaryResponse> getSummary(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest
    ) {
        String driverId = mobileDriverAuthService.requireDriverId(httpRequest);
        service.assertSessionBelongsToDriver(sessionId, driverId);
        return ResponseEntity.ok(service.getSummary(sessionId));
    }

    /** Cerrar turno definitivo */
    @PostMapping("/{sessionId}/close")
    public ResponseEntity<MobileShiftResponse> closeShift(
            @PathVariable String sessionId,
            @Valid @RequestBody CloseShiftMobileRequest request,
            HttpServletRequest httpRequest
    ) {
        String driverId = mobileDriverAuthService.requireDriverId(httpRequest);
        service.assertSessionBelongsToDriver(sessionId, driverId);
        log.info("Cierre de turno: session={}, driver={}, kmFinal={}", sessionId, driverId, request.getKmFinal());
        return ResponseEntity.ok(service.closeShift(sessionId, request));
    }

    /** Registrar ubicación GPS del conductor durante un turno activo */
    @PostMapping("/{sessionId}/location")
    public ResponseEntity<ShiftLocationResponse> saveLocation(
            @PathVariable String sessionId,
            @Valid @RequestBody MobileShiftLocationRequest request,
            HttpServletRequest httpRequest
    ) {
        String driverId = mobileDriverAuthService.requireDriverId(httpRequest);
        service.assertSessionBelongsToDriver(sessionId, driverId);
        return ResponseEntity.ok(locationService.saveLocation(sessionId, request));
    }

    /** Turno activo del conductor */
    @GetMapping("/active")
    public ResponseEntity<MobileShiftResponse> getActive(HttpServletRequest httpRequest) {
        String driverId = mobileDriverAuthService.requireDriverId(httpRequest);
        return service.findActiveByDriver(driverId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/active/{driverId}")
    public ResponseEntity<MobileShiftResponse> getActiveLegacy(
            @PathVariable String driverId,
            HttpServletRequest httpRequest
    ) {
        String authenticatedDriverId = mobileDriverAuthService.requireDriverId(httpRequest);
        if (!authenticatedDriverId.equals(driverId)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar turnos de otro conductor");
        }
        return service.findActiveByDriver(authenticatedDriverId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** Historial de turnos del conductor */
    @GetMapping("/history")
    public ResponseEntity<List<MobileShiftResponse>> getHistory(HttpServletRequest httpRequest) {
        String driverId = mobileDriverAuthService.requireDriverId(httpRequest);
        return ResponseEntity.ok(service.getDriverHistory(driverId));
    }

    @GetMapping({"/driver/{driverId}", "/driver/{driverId}/history"})
    public ResponseEntity<List<MobileShiftResponse>> getHistoryLegacy(
            @PathVariable String driverId,
            HttpServletRequest httpRequest
    ) {
        String authenticatedDriverId = mobileDriverAuthService.requireDriverId(httpRequest);
        if (!authenticatedDriverId.equals(driverId)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes consultar turnos de otro conductor");
        }
        return ResponseEntity.ok(service.getDriverHistory(authenticatedDriverId));
    }
}
