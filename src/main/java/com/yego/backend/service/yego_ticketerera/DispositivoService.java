package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearDispositivoRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.DispositivoResponse;

import java.util.List;
import java.util.Map;

public interface DispositivoService {

    List<DispositivoResponse> listarDispositivos();

    DispositivoResponse crearDispositivo(CrearDispositivoRequest request);

    DispositivoResponse actualizarDispositivo(Long id, CrearDispositivoRequest request);

    DispositivoResponse regenerarTokenAcceso(Long id);

    /**
     * Asigna o desasigna el módulo de una Tablet de Calificación.
     * Si {@code moduleId} es {@code null}, se desvincula del módulo actual.
     */
    DispositivoResponse asignarModulo(Long dispositivoId, Long moduleId);

    void desactivarDispositivo(Long id);

    Map<String, Object> autenticarDispositivo(String accessToken);

    Map<String, Object> refrescarSesionDispositivo(String jwt);

    String generarTokenAcceso();
}
