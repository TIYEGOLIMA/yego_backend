package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.RendimientoResponse;

import java.time.LocalDate;

public interface RendimientoService {
    RendimientoResponse getRendimiento(String periodo, LocalDate weekStart, Integer mes, Integer anio);
}
