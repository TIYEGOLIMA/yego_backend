package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.DriverListResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverSimpleResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriversInOrderResponse;

import java.util.List;

public interface FleetDriverService {
    /**
     * 📋 USO INTERNO: CalculatedShiftService
     * Obtiene la lista de conductores desde la API de Yango
     */
    DriverListResponse obtenerListaConductores(List<String> workRuleIds);
    
    /**
     * 🚗 VISTA: MonitoreoEnVivoView
     * Obtiene todos los conductores con status "in_order" y "free" con paginación
     * @param page Número de página (0-indexed)
     * @param limit Cantidad de conductores por página
     * @return Respuesta con lista de conductores en orden y sus detalles
     */
    DriversInOrderResponse obtenerConductoresEnOrden(Integer page, Integer limit);
    
    /**
     * 🚗 VISTA: MonitoreoEnVivoView
     * Obtiene todos los conductores con status "in_order" y "free" (sin paginación)
     * @return Respuesta con lista de conductores en orden y sus detalles
     */
    default DriversInOrderResponse obtenerConductoresEnOrden() {
        return obtenerConductoresEnOrden(0, Integer.MAX_VALUE);
    }
    
    /**
     * 📋 ENDPOINT: Lista de conductores simplificada
     * Obtiene una lista de todos los conductores con solo: nombre, telefono, driver_id y avatar_url
     * @return Lista de conductores con información básica
     */
    DriverSimpleResponse obtenerListaConductoresSimplificada();
    
    }

