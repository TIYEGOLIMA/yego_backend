package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearSedeRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.SedeResponse;

import java.util.List;

public interface SedeService {

    List<SedeResponse> listarSedes();

    SedeResponse obtenerSede(Long id);

    SedeResponse crearSede(CrearSedeRequest request);

    SedeResponse actualizarSede(Long id, CrearSedeRequest request);

    void desactivarSede(Long id);
}
