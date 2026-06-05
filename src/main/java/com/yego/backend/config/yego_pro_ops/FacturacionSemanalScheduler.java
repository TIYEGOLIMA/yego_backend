package com.yego.backend.config.yego_pro_ops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FacturacionSemanalScheduler {

    /**
     * La facturación semanal automática ha sido deshabilitada porque dependía de
     * CalculatedShiftService (turnos por día, ahora deprecados).
     * Re-implementar usando ShiftSessionService para agrupar viajes por sesiones liquidables.
     */
    public void generarFacturacionSemanalAutomatica() {
        log.info("[FacturacionScheduler] Deshabilitado: migrar a shift_sessions");
    }
}
