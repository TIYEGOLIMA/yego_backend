package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.RegistroCompletoResponse;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;

import java.util.List;

public interface YegoGarantizadoRegistroService {

    List<YegoGarantizado> procesarConductoresPorSemana(String semana);
    GarantizadoListResponse obtenerGarantizadosPorFlota(String flotaId);
    GarantizadoListResponse procesarYDevolverSemanaAnterior();
    GarantizadoListResponse listarGarantizadosSemanaAnterior();
    List<RegistroCompletoResponse> obtenerRegistrosSemanaActualCompletos();
    byte[] exportarExcel(String flotaId, String estado, String semana);
    boolean marcarComoPagado(Long id);
}
