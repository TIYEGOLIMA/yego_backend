package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.RendimientoResponse;
import com.yego.backend.service.yego_pro_ops.RendimientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/pro-ops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RendimientoController {

    private final RendimientoService rendimientoService;

    @GetMapping("/rendimiento")
    public RendimientoResponse getRendimiento(
            @RequestParam(defaultValue = "semanal") String periodo,
            @RequestParam(required = false) String weekStart,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio) {
        LocalDate ws = weekStart != null ? LocalDate.parse(weekStart) : null;
        log.info("[RendimientoController] periodo={} weekStart={} mes={} anio={}", periodo, ws, mes, anio);
        return rendimientoService.getRendimiento(periodo, ws, mes, anio);
    }
}
