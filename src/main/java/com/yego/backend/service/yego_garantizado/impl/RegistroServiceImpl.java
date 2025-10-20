package com.yego.backend.service.yego_garantizado.impl;

import com.yego.backend.entity.yego_garantizado.api.request.RegistroRequest;
import com.yego.backend.entity.yego_garantizado.api.response.RegistroResponse;
import com.yego.backend.entity.yego_garantizado.entities.Registro;
import com.yego.backend.repository.yego_garantizado.RegistroRepository;
import com.yego.backend.service.yego_garantizado.DriverService;
import com.yego.backend.service.yego_garantizado.RegistroService;
import com.yego.backend.service.yego_garantizado.SystemStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio para operaciones relacionadas con registros de garantizado
 * Maneja la creación y gestión de registros de conductores
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Service
@Slf4j
public class RegistroServiceImpl implements RegistroService {

    private final RegistroRepository registroRepository;
    private final DriverService driverService;
    private final SystemStatusService systemStatusService;

    public RegistroServiceImpl(RegistroRepository registroRepository, DriverService driverService, SystemStatusService systemStatusService) {
        this.registroRepository = registroRepository;
        this.driverService = driverService;
        this.systemStatusService = systemStatusService;
    }

    @Override
    public RegistroResponse crearRegistro(RegistroRequest request) {
        // Validar que el sistema esté activo
        if (!systemStatusService.isSystemActive()) {
            throw new IllegalStateException("El sistema está temporalmente desactivado. Horario de atención: Lunes a Viernes de 6:00 AM a 11:59 PM");
        }

        // Validar términos y condiciones
        if (!request.getYegTerminosAceptados()) {
            throw new IllegalArgumentException("Debe aceptar los términos y condiciones");
        }

        // Validar que la licencia existe en la tabla drivers
        String licencia = request.getYegLicenciaNumero();
        log.info("Validando licencia: {}", licencia);
        
        var driverInfo = driverService.validarYObtenerLicencia(licencia);
        
        if (driverInfo.isEmpty()) {
            log.warn("La licencia {} no existe en la tabla drivers", licencia);
            throw new IllegalArgumentException("La licencia " + licencia + " no existe en el sistema");
        }

        log.info("Licencia {} validada correctamente - Conductor: {}", licencia, driverInfo.get().getFullName());

        Registro registro = new Registro();
        registro.setYegLicenciaNumero(request.getYegLicenciaNumero());
        registro.setYegFlota(request.getYegFlota());
        registro.setYegSemana(obtenerSemanaActual());
        registro.setYegTerminosAceptados(request.getYegTerminosAceptados());

        Registro registroGuardado = registroRepository.save(registro);
        log.info("Registro creado exitosamente con ID: {}", registroGuardado.getYegId());

        return convertirAResponse(registroGuardado, "Registro creado exitosamente para " + driverInfo.get().getFullName());
    }

    private RegistroResponse convertirAResponse(Registro entity, String mensaje) {
        RegistroResponse response = new RegistroResponse();
        BeanUtils.copyProperties(entity, response);
        response.setMensaje(mensaje);
        return response;
    }

    private String obtenerSemanaActual() {
        return "SEMANA" + (java.time.LocalDateTime.now().getDayOfYear() / 7 + 1);
    }
}
