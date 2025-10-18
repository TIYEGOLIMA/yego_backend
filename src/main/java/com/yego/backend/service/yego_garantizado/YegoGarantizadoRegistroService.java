package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoResponse;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizado;
import com.yego.backend.entity.yego_garantizado.entities.YegoGarantizadoRegistro;

import java.util.List;

public interface YegoGarantizadoRegistroService {

    List<YegoGarantizado> procesarConductoresPorSemana(String semana);

    List<YegoGarantizado> procesarSemanaActual();

    GarantizadoListResponse obtenerGarantizadosPorFlota(String flotaId);

    GarantizadoListResponse procesarYDevolverSemanaActual();

    byte[] exportarExcel(String flotaId, String estado, String semana);
}
