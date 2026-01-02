package com.yego.backend.service.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftManualRequest;
import com.yego.backend.entity.yego_pro_ops.api.request.CalculatedShiftRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.AllDriversOrdersResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.CalculatedShiftResponse;
import com.yego.backend.entity.yego_pro_ops.api.response.FechasConTiposTurnoResponse;
import com.yego.backend.entity.yego_pro_ops.entities.CalculatedShift;

public interface CalculatedShiftService {
    CalculatedShiftResponse guardarTurnos(CalculatedShiftRequest request);
    CalculatedShiftResponse listarTurnos(String driverId, String fecha);
    void actualizarTurnoConductor(String driverId, String status, Integer statusDuration, java.time.LocalDateTime primeraVezVistoActivoHoy);
    void finalizarTurnosActivos(String driverId, java.time.LocalDate fecha);
    void verificarYFinalizarTurnosDesconectados(java.util.Set<String> conductoresActivos);
    
    /**
     * Calcula y guarda las horas de turno para un conductor y fecha específica basándose en los viajes
     * @param driverId ID del conductor
     * @param fecha Fecha del turno
     */
    void calcularYGuardarHorasTurno(String driverId, java.time.LocalDate fecha);
    
    /**
     * Procesa las horas de turno del día anterior para todos los conductores activos
     */
    void procesarHorasTurnoDiaAnterior();
    
    /**
     * Guarda un turno manual ingresado por el usuario
     * @param request Request con los datos del turno manual
     * @return Turno guardado
     */
    CalculatedShift guardarTurnoManual(CalculatedShiftManualRequest request);
    
    /**
     * Obtiene todos los conductores con todos sus viajes en vivo para una fecha específica
     * @param fecha Fecha en formato "yyyy-MM-dd" (opcional, si es null usa la fecha actual)
     * @return Respuesta con lista de conductores y sus viajes
     */
    AllDriversOrdersResponse listarTodosLosConductoresConViajes(String fecha);
    
    /**
     * Obtiene las fechas únicas con sus tipos de turno para un conductor
     * @param driverId ID del conductor
     * @return Respuesta con fechas únicas y sus tipos de turno (diurno, nocturno, o ambos)
     */
    FechasConTiposTurnoResponse obtenerFechasConTiposTurno(String driverId);
}

