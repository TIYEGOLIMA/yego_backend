package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.service.yego_garantizado.SystemStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SystemStatusServiceImpl implements SystemStatusService {

    private volatile boolean systemActive = false; // Sistema inicia inactivo
    private volatile String lastStatusChange = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    @Override
    public boolean isSystemActive() {
        // Verificar automáticamente si el sistema debería estar activo según el horario
        checkAndUpdateSystemStatus();
        return systemActive;
    }
    
    /**
     * Verifica si el sistema debería estar activo según el horario laboral
     * y actualiza el estado si es necesario
     */
    private void checkAndUpdateSystemStatus() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();
        
        boolean shouldBeActive = false;
        
        // Verificar si está en horario laboral (lunes 6:00 AM - viernes 23:59)
        if (currentDay == DayOfWeek.TUESDAY || currentDay == DayOfWeek.WEDNESDAY || 
            currentDay == DayOfWeek.THURSDAY) {
            shouldBeActive = true;
        } else if (currentDay == DayOfWeek.MONDAY) {
            // Lunes: activo solo después de las 6:00 AM
            shouldBeActive = currentTime.isAfter(LocalTime.of(6, 0));
        } else if (currentDay == DayOfWeek.FRIDAY) {
            // Viernes: activo hasta las 23:59
            shouldBeActive = currentTime.isBefore(LocalTime.of(23, 59));
        } else if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
            // Fin de semana: inactivo
            shouldBeActive = false;
        }
        
        // Actualizar estado si es diferente al actual
        if (shouldBeActive != systemActive) {
            synchronized (this) {
                systemActive = shouldBeActive;
                lastStatusChange = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (shouldBeActive) {
                    log.info("✅ Sistema ACTIVADO automáticamente - Timestamp: {}", lastStatusChange);
                } else {
                    log.warn("❌ Sistema DESACTIVADO automáticamente - Timestamp: {}", lastStatusChange);
                }
            }
        }
    }

    @Override
    public void activateSystem() {
        synchronized (this) {
            systemActive = true;
            lastStatusChange = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            log.info("✅ Sistema ACTIVADO - Timestamp: {}", lastStatusChange);
        }
    }

    @Override
    public void deactivateSystem() {
        synchronized (this) {
            systemActive = false;
            lastStatusChange = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            log.warn("❌ Sistema DESACTIVADO - Timestamp: {}", lastStatusChange);
        }
    }

    @Override
    public String getCurrentStatus() {
        return systemActive ? "ACTIVO" : "INACTIVO";
    }

    @Override
    public String getNextActivationTime() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalDateTime nextActivation;
        
        if (currentDay == DayOfWeek.SATURDAY) {
            // Si es sábado, próxima activación es lunes 6:00 AM
            nextActivation = now.plusDays(2).with(LocalTime.of(6, 0));
        } else if (currentDay == DayOfWeek.SUNDAY) {
            // Si es domingo, próxima activación es lunes 6:00 AM
            nextActivation = now.plusDays(1).with(LocalTime.of(6, 0));
        } else if (currentDay == DayOfWeek.FRIDAY && now.toLocalTime().isAfter(LocalTime.of(23, 59))) {
            // Si es viernes después de 23:59, próxima activación es lunes 6:00 AM
            nextActivation = now.plusDays(3).with(LocalTime.of(6, 0));
        } else {
            // Si es lunes antes de 6:00 AM, próxima activación es hoy 6:00 AM
            nextActivation = now.with(LocalTime.of(6, 0));
        }
        
        return nextActivation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @Override
    public String getNextDeactivationTime() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalDateTime nextDeactivation;
        
        if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
            // Si es fin de semana, próxima desactivación es viernes 23:59
            int daysUntilFriday = (DayOfWeek.FRIDAY.getValue() - currentDay.getValue() + 7) % 7;
            nextDeactivation = now.plusDays(daysUntilFriday).with(LocalTime.of(23, 59));
        } else {
            // Si es día laboral, próxima desactivación es viernes 23:59
            int daysUntilFriday = (DayOfWeek.FRIDAY.getValue() - currentDay.getValue() + 7) % 7;
            nextDeactivation = now.plusDays(daysUntilFriday).with(LocalTime.of(23, 59));
        }
        
        return nextDeactivation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
