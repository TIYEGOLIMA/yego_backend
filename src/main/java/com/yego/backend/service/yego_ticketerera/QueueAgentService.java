package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.AsignarModuloResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModulosEstadoResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.RecuperarModuloResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

public interface QueueAgentService {

    ResponseEntity<Map<String, Object>> liberarModuloDelUsuario(Long userId);

    ResponseEntity<Map<String, Object>> liberarModuloPorModuleId(Long moduleId);

    Optional<Long> obtenerQueueAgentIdPorUsuario(Long userId);

    Optional<RecuperarModuloResponse> recuperarModuloAsignado(Long userId);

    ModulosEstadoResponse obtenerModulosDisponiblesYOcupados();

    ResponseEntity<AsignarModuloResponse> asignarModuloAUsuario(Map<String, Object> request);
}
