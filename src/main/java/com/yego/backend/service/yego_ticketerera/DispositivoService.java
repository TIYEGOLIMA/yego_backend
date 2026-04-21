package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearDispositivoRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.DispositivoResponse;

import java.util.List;
import java.util.Map;

public interface DispositivoService {

    List<DispositivoResponse> listarDispositivos();

    List<DispositivoResponse> listarDispositivosPorSede(Long sedeId);

    DispositivoResponse obtenerDispositivo(Long id);

    DispositivoResponse crearDispositivo(CrearDispositivoRequest request);

    DispositivoResponse actualizarDispositivo(Long id, CrearDispositivoRequest request);

    void desactivarDispositivo(Long id);

    Map<String, Object> autenticarDispositivo(String accessToken);

    String generarTokenAcceso();
}
