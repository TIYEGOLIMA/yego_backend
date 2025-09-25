package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.DniValidationDto;

/**
 * Interfaz del servicio de validación de DNI del sistema YEGO Principal
 * Equivalente a DniValidationService de NestJS
 */
public interface DniValidationService {
    
    /**
     * Validar DNI con API externa
     */
    DniValidationDto validateDni(String dni);
    
    /**
     * Formatear datos de usuario desde DNI
     */
    DniValidationDto.DniDataDto formatUserData(Object dniData);
}
