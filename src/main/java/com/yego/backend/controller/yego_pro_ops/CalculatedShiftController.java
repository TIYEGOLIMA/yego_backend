package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftManualRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.CalculatedShiftResponse;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;
import com.yego.backend.service.yego_pro_ops.CalculatedShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pro-ops/shifts")
@RequiredArgsConstructor
public class CalculatedShiftController {

    private final CalculatedShiftService calculatedShiftService;

    @PostMapping
    public ResponseEntity<CalculatedShiftResponse> guardarTurnos(@Valid @RequestBody CalculatedShiftRequest request) {
        return ResponseEntity.ok(calculatedShiftService.guardarTurnos(request));
    }

    @GetMapping
    public ResponseEntity<CalculatedShiftResponse> listarTurnos(
            @RequestParam String driver_id,
            @RequestParam String fecha) {
        return ResponseEntity.ok(calculatedShiftService.listarTurnos(driver_id, fecha));
    }

    /**
     * Guarda un turno manual ingresado por el usuario
     * @param request Datos del turno manual (conductorId, fecha, horaInicio, horaFin, tipoTurno)
     * @return Turno guardado
     */
    @PostMapping("/manual")
    public ResponseEntity<CalculatedShift> guardarTurnoManual(@Valid @RequestBody CalculatedShiftManualRequest request) {
        log.info("📝 [CalculatedShiftController] Guardando turno manual para driverId: {}, fecha: {}", 
            request.getDriverId(), request.getFecha());
        CalculatedShift turno = calculatedShiftService.guardarTurnoManual(request);
        log.info("✅ [CalculatedShiftController] Turno manual guardado exitosamente con ID: {}", turno.getId());
        return ResponseEntity.ok(turno);
    }
}

