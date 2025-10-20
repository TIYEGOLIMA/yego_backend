package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.ModuleRequest;
import com.yego.backend.entity.yego_principal.api.response.ModuleResponse;
import com.yego.backend.entity.yego_principal.entities.Module;

import java.util.List;

public interface ModuleService {
    
    List<ModuleResponse> obtenerTodos();
    
    List<ModuleResponse> obtenerActivos();
    
    ModuleResponse obtenerPorId(Long id);
    
    ModuleResponse crear(ModuleRequest request);
    
    ModuleResponse actualizar(Long id, ModuleRequest request);
    
    void eliminar(Long id);
    
    void toggleActive(Long id);
}
