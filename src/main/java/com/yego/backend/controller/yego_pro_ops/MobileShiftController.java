package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.mobile.OpenShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.mobile.CloseShiftMobileRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.mobile.MobileShiftSummaryResponse;
import com.yego.backend.service.yego_pro_ops.mobile.MobileShiftService;
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

    /** Abrir turno desde la app móvil */
    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MobileShiftResponse> openShift(
            @Valid @RequestBody OpenShiftMobileRequest request
    ) {
        log.info("Apertura de turno: driver={}, placa={}", request.getDriverId(), request.getPlaca());
        MobileShiftResponse response = service.openShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Obtener resumen Yango (viajes, producido, efectivo) ANTES de confirmar cierre */
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<MobileShiftSummaryResponse> getSummary(
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(service.getSummary(sessionId));
    }

    /** Cerrar turno definitivo */
    @PostMapping("/{sessionId}/close")
    public ResponseEntity<MobileShiftResponse> closeShift(
            @PathVariable String sessionId,
            @Valid @RequestBody CloseShiftMobileRequest request
    ) {
        log.info("Cierre de turno: session={}, kmFinal={}", sessionId, request.getKmFinal());
        return ResponseEntity.ok(service.closeShift(sessionId, request));
    }

    /** Turno activo del conductor */
    @GetMapping("/active/{driverId}")
    public ResponseEntity<MobileShiftResponse> getActive(@PathVariable String driverId) {
        return service.findActiveByDriver(driverId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /** Historial de turnos del conductor */
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<MobileShiftResponse>> getHistory(@PathVariable String driverId) {
        return ResponseEntity.ok(service.getDriverHistory(driverId));
    }
}
