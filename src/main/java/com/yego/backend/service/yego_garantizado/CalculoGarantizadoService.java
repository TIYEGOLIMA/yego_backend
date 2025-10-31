package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.request.CalcularGarantizadoRequest;
import com.yego.backend.entity.yego_garantizado.api.response.GarantizadoListResponse;

/**
 * Interfaz del servicio de cálculos de garantizado
 */
public interface CalculoGarantizadoService {
    
    /**
     * Guardar múltiples configuraciones desde el request completo del frontend
     * Procesa el JSON (suma viajes/bonos, toma máximo de horas) y guarda cada ciudad
     * Si no viene semana, usa la semana anterior automáticamente
     * @param request Request completo con países, ciudades y cálculos
     */
    void guardarConfiguraciones(CalcularGarantizadoRequest request);
    
    /**
     * Guardar configuraciones Y PROCESAR conductores en un solo método
     * @param request Request completo con países, ciudades y cálculos
     * @return Lista de conductores procesados
     */
    GarantizadoListResponse guardarConfiguracionesYProcesar(CalcularGarantizadoRequest request);
}

