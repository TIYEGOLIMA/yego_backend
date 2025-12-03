package com.yego.backend.service.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.response.DriverInfo;
import com.yego.backend.entity.yego_garantizado.api.response.DriverValidationResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaDisponibleResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_api_externo.api.response.PPendientesResponse;

import java.util.List;
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
    
    /**
     * Obtiene información de pagos pendientes para GoBot
     * @param telefono número de teléfono del conductor
     * @return Respuesta con información de pagos pendientes
     */
    PPendientesResponse obtenerPendientes(String telefono);
    
    /**
     * Obtiene todas las flotas desde la API externa (sin filtrar)
     * @return Lista de todas las flotas disponibles
     */
    List<FlotaResponse> obtenerFlotasPendientes();
}

