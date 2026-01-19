package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.DriverPaymentSummaryResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.PaidShiftsResponse;

public interface CalculatedShiftService {
    /**
     * 🔧 USO INTERNO: Scheduler
     * Calcula y guarda las horas de turno para un conductor y fecha específica basándose en los viajes
     * @param driverId ID del conductor
     * @param fecha Fecha del turno
     */
    void calcularYGuardarHorasTurno(String driverId, java.time.LocalDate fecha);
    
    /**
     * 🔧 USO INTERNO: Scheduler (diario a las 5 AM)
     * Procesa las horas de turno del día anterior para todos los conductores activos
     */
    void procesarHorasTurnoDiaAnterior();
    
    /**
     * 📋 VISTA: DetalleView
     * Obtiene las fechas únicas con sus tipos de turno para un conductor
     * @param driverId ID del conductor
     * @return Respuesta con fechas únicas y sus tipos de turno (diurno, nocturno, o ambos)
     */
    FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId);
    
    /**
     * 📋 VISTA: Resumen de Pagos
     * Obtiene el resumen de pagos de todos los conductores con sus turnos filtrados por fecha
     * @param fecha Fecha para filtrar los turnos (formato: "YYYY-MM-DD")
     * @return Respuesta con lista de conductores, monto total a pagar, cantidad de turnos y lista de turnos
     */
    DriverPaymentSummaryResponse obtenerResumenPagos(String fecha);
    
    /**
     * 💰 VISTA: Lista de Turnos Pagados
     * Obtiene todos los turnos que ya están pagados (pagado = true)
     * @param fecha Fecha opcional para filtrar los turnos pagados (formato: "YYYY-MM-DD", null para todos)
     * @return Respuesta con lista de turnos pagados
     */
    PaidShiftsResponse obtenerTurnosPagados(String fecha);
}

