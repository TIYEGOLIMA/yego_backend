package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.DriverOrdersRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.MultipleDriversTripsSimplifiedResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.OrderInfoResponse;

import java.util.List;

public interface DriverOrdersService {
    /**
     * 🔧 USO INTERNO: CalculatedShiftService
     * Obtiene las órdenes del día actual para cálculos internos de turnos
     * NO se usa directamente desde las vistas del frontend
     */
    DriverOrdersResponse obtenerOrdenesDelDia(DriverOrdersRequest request);
    
    /**
     * 📋 VISTA: DetalleView
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
     * 🔧 USO INTERNO: CalculatedShiftService
     * Obtiene TODOS los viajes (completos y cancelados) sin filtrar por status
     * Útil para cálculos de turnos donde se necesitan todos los viajes para determinar hora de inicio y fin
     * NO se usa directamente desde las vistas del frontend
     * @param driverId ID del conductor
     * @param dateFrom Fecha inicial (formato: "2025-12-10T00:00:00-05:00")
     * @param dateTo Fecha final (formato: "2025-12-10T23:59:59-05:00")
     * @param cursor Cursor opcional para paginación (obtenido de la respuesta anterior)
     * @return Respuesta con lista de TODOS los viajes (sin filtrar por status)
     */
    DriverOrdersResponse obtenerTodosLosViajes(String driverId, String dateFrom, String dateTo, String cursor);
    
    /**
     * 🔧 USO INTERNO: WebSocket (FleetDriverNotificationHandler)
     * Obtiene viajes simplificados para múltiples conductores en una sola llamada (procesamiento en paralelo)
     * Se usa para enviar datos por WebSocket cada 5 minutos, NO directamente desde las vistas del frontend
     * @param driverIds Lista de IDs de conductores
     * @param dateFrom Fecha inicial (formato: "2025-12-10T00:00:00-05:00")
     * @param dateTo Fecha final (formato: "2025-12-10T23:59:59-05:00")
     * @return Respuesta con viajes agrupados por conductor
     */
    MultipleDriversTripsSimplifiedResponse obtenerViajesSimplificadosMultiples(List<String> driverIds, String dateFrom, String dateTo);
    
    /**
     * 🚗 VISTA: MonitoreoEnVivoView
     * Obtiene viajes simplificados para un conductor en una fecha específica
     * Calcula automáticamente el rango del día (00:00:00 a 23:59:59) en zona horaria de Lima
     * Usado para mostrar viajes de "Ayer" en el modal de la vista de monitoreo
     * @param driverId ID del conductor
     * @param fecha Fecha en formato "YYYY-MM-DD" (ej: "2025-12-10")
     * @return Respuesta con viajes simplificados del conductor
     */
    DriverTripsSimplifiedResponse obtenerViajesSimplificadosPorFecha(String driverId, String fecha);
    
}

