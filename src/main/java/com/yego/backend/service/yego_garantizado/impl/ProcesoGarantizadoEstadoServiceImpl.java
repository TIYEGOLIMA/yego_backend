package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.response.EstadoProcesoResponse;
import com.yego.backend.entity.yego_garantizado.entities.ProcesoGarantizadoEstado;
import com.yego.backend.repository.yego_garantizado.ProcesoGarantizadoEstadoRepository;
import com.yego.backend.service.yego_garantizado.ProcesoGarantizadoEstadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Implementación del servicio de estado del procesamiento
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcesoGarantizadoEstadoServiceImpl implements ProcesoGarantizadoEstadoService {

    private final ProcesoGarantizadoEstadoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public EstadoProcesoResponse obtenerEstadoProceso() {
        LocalDateTime ahora = LocalDateTime.now();
        DayOfWeek diaActual = ahora.getDayOfWeek();
        LocalTime horaActual = ahora.toLocalTime();
        
        Optional<ProcesoGarantizadoEstado> estadoOpt = repository.findFirstByOrderByIdDesc();
        
        // Si no existe registro previo y es lunes después de las 9 AM, está disponible
        if (estadoOpt.isEmpty()) {
            boolean disponible = esLunesDespuesDeNueveAM(diaActual, horaActual);
            return EstadoProcesoResponse.builder()
                    .bloqueado(!disponible)
                    .ultimoProcesamiento(null)
                    .mensaje(disponible ? "Listo para procesar" : "Disponible solo los lunes a las 9:00 AM o después")
                    .proximoLunes(obtenerProximoLunes())
                    .build();
        }
        
        ProcesoGarantizadoEstado estado = estadoOpt.get();
        LocalDateTime ultimoProcesamiento = estado.getUltimoProcesamiento();
        LocalDateTime proximoLunes = obtenerProximoLunes();
        
        // Validar si es lunes y son las 9 AM o después
        boolean esLunesNueveAM = esLunesDespuesDeNueveAM(diaActual, horaActual);
        
        // Si no es lunes después de las 9 AM, está bloqueado
        if (!esLunesNueveAM) {
            long diasHastaLunes = ChronoUnit.DAYS.between(ahora.toLocalDate(), proximoLunes.toLocalDate());
            String mensaje = diasHastaLunes == 0 
                ? "Disponible a las 9:00 AM de hoy"
                : String.format("Disponible el próximo lunes a las 9:00 AM (en %d días)", diasHastaLunes);
            
            return EstadoProcesoResponse.builder()
                    .bloqueado(true)
                    .ultimoProcesamiento(ultimoProcesamiento)
                    .mensaje(mensaje)
                    .proximoLunes(proximoLunes)
                    .build();
        }
        
        // Si es lunes después de las 9 AM, verificar si ya se procesó esta semana
        LocalDateTime ultimoLunes = obtenerUltimoLunes();
        
        // Si el último procesamiento fue después del último lunes, está bloqueado hasta el próximo lunes
        if (ultimoProcesamiento.isAfter(ultimoLunes) || ultimoProcesamiento.equals(ultimoLunes)) {
            return EstadoProcesoResponse.builder()
                    .bloqueado(true)
                    .ultimoProcesamiento(ultimoProcesamiento)
                    .mensaje("Ya se procesó esta semana. Disponible el próximo lunes.")
                    .proximoLunes(proximoLunes)
                    .build();
        }
        
        // Si llegamos aquí, es lunes después de las 9 AM y no se ha procesado esta semana
        return EstadoProcesoResponse.builder()
                .bloqueado(false)
                .ultimoProcesamiento(ultimoProcesamiento)
                .mensaje("Listo para procesar")
                .proximoLunes(proximoLunes)
                .build();
    }
    
    /**
     * Verifica si es lunes y son las 9:00 AM o después
     */
    private boolean esLunesDespuesDeNueveAM(DayOfWeek dia, LocalTime hora) {
        boolean esLunes = dia == DayOfWeek.MONDAY;
        boolean esNueveAMOdespues = hora.isAfter(LocalTime.of(9, 0)) || hora.equals(LocalTime.of(9, 0));
        return esLunes && esNueveAMOdespues;
    }

    @Override
    @Transactional
    public void registrarProcesamiento() {
        log.info("📝 [ProcesoGarantizadoEstadoService] Registrando nuevo procesamiento");
        
        LocalDateTime ahora = LocalDateTime.now();
        
        // Buscar si ya existe un registro
        Optional<ProcesoGarantizadoEstado> estadoOpt = repository.findFirstByOrderByIdDesc();
        
        ProcesoGarantizadoEstado estado;
        if (estadoOpt.isPresent()) {
            // Actualizar el registro existente
            estado = estadoOpt.get();
            log.info("🔄 [ProcesoGarantizadoEstadoService] Actualizando registro existente ID: {}", estado.getId());
        } else {
            // Crear nuevo registro
            estado = new ProcesoGarantizadoEstado();
            log.info("✨ [ProcesoGarantizadoEstadoService] Creando nuevo registro");
        }
        
        estado.setUltimoProcesamiento(ahora);
        estado.setBloqueado(true);
        
        repository.save(estado);
        
        log.info("✅ [ProcesoGarantizadoEstadoService] Procesamiento registrado - Bloqueado hasta el próximo lunes (ID: {})", estado.getId());
    }
    
    /**
     * Obtener el último lunes (inicio de la semana actual)
     */
    private LocalDateTime obtenerUltimoLunes() {
        LocalDateTime ahora = LocalDateTime.now();
        DayOfWeek diaSemana = ahora.getDayOfWeek();
        
        int diasRestantes = 0;
        if (diaSemana == DayOfWeek.SUNDAY) {
            diasRestantes = 6; // Hace 6 días era lunes
        } else {
            diasRestantes = diaSemana.getValue() - 1; // Días desde el lunes
        }
        
        return ahora.minusDays(diasRestantes).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    
    /**
     * Obtener el próximo lunes
     */
    private LocalDateTime obtenerProximoLunes() {
        LocalDateTime ahora = LocalDateTime.now();
        DayOfWeek diaSemana = ahora.getDayOfWeek();
        
        int diasHastaLunes = 0;
        if (diaSemana == DayOfWeek.SUNDAY) {
            diasHastaLunes = 1; // Mañana es lunes
        } else if (diaSemana == DayOfWeek.MONDAY) {
            diasHastaLunes = 7; // En 7 días será el próximo lunes
        } else {
            diasHastaLunes = 8 - diaSemana.getValue(); // Días restantes de la semana + 1
        }
        
        return ahora.plusDays(diasHastaLunes).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}

