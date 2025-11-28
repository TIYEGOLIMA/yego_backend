package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.GrupoRequest;
import com.yego.backend.entity.yego_principal.api.response.GrupoResponse;

import java.util.List;

public interface GrupoService {
    
    List<GrupoResponse> obtenerActivos();
    
    GrupoResponse crear(GrupoRequest request);
    
    GrupoResponse actualizar(Long id, GrupoRequest request);
    
    void eliminar(Long id);
    
    void toggleActive(Long id);
}

