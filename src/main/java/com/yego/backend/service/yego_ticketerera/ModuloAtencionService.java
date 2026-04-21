package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.request.CrearModuloAtencionRequest;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloAtencionResponse;
import com.yego.backend.entity.yego_ticketerera.api.response.ModuloUsuarioResponse;

import java.util.List;

public interface ModuloAtencionService {

    List<ModuloAtencionResponse> obtenerTodosLosModulosActivosResponse();

    List<ModuloAtencionResponse> listarTodos(Long sedeId);

    ModuloAtencionResponse crear(CrearModuloAtencionRequest request);

    ModuloAtencionResponse actualizar(Long id, CrearModuloAtencionRequest request);

    void cambiarEstadoModulo(Long moduleId, boolean activo);

    void eliminar(Long id);

    ModuloUsuarioResponse verificarModuloOListarDisponibles(Long userId, Long sedeId);
}
