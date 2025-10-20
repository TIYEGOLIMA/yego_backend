package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.DriverInfo;
import com.yego.backend.entity.yego_garantizado.api.response.DriverValidationResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaDisponibleResponse;

import java.util.Optional;

/**
 * Servicio para operaciones relacionadas con conductores
 * Maneja validación de licencias y obtención de flotas
 * 
 * @author Sistema Yego
 * @version 1.0
 */
public interface DriverService {
    
    /**
     * Valida si existe una licencia en drivers (uso interno)
     * @param licenseNumber número de licencia a validar
     * @return Optional con la información del conductor si existe
     */
    Optional<DriverInfo> validarYObtenerLicencia(String licenseNumber);
    
    /**
     * Valida si existe una licencia y devuelve los datos básicos
     * @param licenseNumber número de licencia a validar
     * @return Respuesta con datos si existe
     */
    DriverValidationResponse validarLicencia(String licenseNumber);
    
    /**
     * Obtiene conductor con TODAS sus flotas disponibles (filtradas)
     * @param licenseNumber número de licencia a buscar
     * @return Conductor con lista de flotas disponibles
     */
    FlotaDisponibleResponse obtenerConductorConFlotas(String licenseNumber);
}
