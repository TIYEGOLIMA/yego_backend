package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.request.DniValidationDto;
import com.yego.backend.service.yego_principal.DniValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de validación de DNI del sistema YEGO Principal
 * Equivalente a DniValidationService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DniValidationServiceImpl implements DniValidationService {
    
    @Override
    public DniValidationDto validateDni(String dni) {
        try {
            // Simulación de validación de DNI
            // En un caso real, aquí se haría la llamada a la API externa
            
            if (dni == null || dni.trim().isEmpty()) {
                return DniValidationDto.builder()
                        .success(false)
                        .message("DNI no proporcionado")
                        .build();
            }
            
            if (dni.length() != 8 || !dni.matches("\\d+")) {
                return DniValidationDto.builder()
                        .success(false)
                        .message("DNI debe tener 8 dígitos")
                        .build();
            }
            
            // Simulación de datos encontrados
            DniValidationDto.DniDataDto data = DniValidationDto.DniDataDto.builder()
                    .dni(dni)
                    .nombres("Juan Carlos")
                    .apellidoPaterno("Pérez")
                    .apellidoMaterno("García")
                    .nombreCompleto("Juan Carlos Pérez García")
                    .fechaNacimiento("15/03/1990")
                    .sexo("M")
                    .build();
            
            log.info("✅ DNI validado exitosamente en YEGO Principal: {}", dni);
            
            return DniValidationDto.builder()
                    .success(true)
                    .message("DNI validado exitosamente")
                    .data(data)
                    .build();
            
        } catch (Exception e) {
            log.error("❌ Error validando DNI en YEGO Principal {}: {}", dni, e.getMessage());
            
            return DniValidationDto.builder()
                    .success(false)
                    .message("Error en la validación del DNI: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public DniValidationDto.DniDataDto formatUserData(Object dniData) {
        // En un caso real, aquí se formatearían los datos de la API externa
        // Por ahora retornamos datos simulados
        
        return DniValidationDto.DniDataDto.builder()
                .dni("12345678")
                .nombres("Usuario")
                .apellidoPaterno("Ejemplo")
                .apellidoMaterno("Prueba")
                .nombreCompleto("Usuario Ejemplo Prueba")
                .fechaNacimiento("01/01/1990")
                .sexo("M")
                .build();
    }
}

