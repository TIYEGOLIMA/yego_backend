package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;

import java.util.List;

public interface DriverOrdersService {
    DriverOrdersResponse obtenerOrdenesDelDia(DriverOrdersRequest request);
    
    /**
     * Obtiene solo los viajes completos con atributos específicos (distancia, efectivo, tarjeta, precio)
     * Incluye un campo booleano que indica si existe un cierre de caja registrado para esa fecha
     * @param driverId ID del conductor
     * @param dateFrom Fecha inicial (formato: "2025-12-10T00:00:00-05:00")
     * @param dateTo Fecha final (formato: "2025-12-10T23:59:59-05:00")
     * @param cursor Cursor opcional para paginación (obtenido de la respuesta anterior)
     * @return Respuesta con lista de viajes completos, cursor para la siguiente página (si existe)
     *         y un flag indicando si hay un cierre registrado para esa fecha
     */
    DriverOrdersResponse obtenerViajesCompletos(String driverId, String dateFrom, String dateTo, String cursor);
    
    /**
     * Obtiene TODOS los viajes (completos y cancelados) sin filtrar por status
     * Útil para cálculos de turnos donde se necesitan todos los viajes para determinar hora de inicio y fin
     * @param driverId ID del conductor
     * @param dateFrom Fecha inicial (formato: "2025-12-10T00:00:00-05:00")
     * @param dateTo Fecha final (formato: "2025-12-10T23:59:59-05:00")
     * @param cursor Cursor opcional para paginación (obtenido de la respuesta anterior)
     * @return Respuesta con lista de TODOS los viajes (sin filtrar por status)
     */
    DriverOrdersResponse obtenerTodosLosViajes(String driverId, String dateFrom, String dateTo, String cursor);
}

