package com.yego.backend.controller.yego_garantizado;

import com.yego.backend.entity.yego_garantizado.api.request.RegistroRequest;
import com.yego.backend.entity.yego_garantizado.api.response.RegistroResponse;
import com.yego.backend.service.yego_garantizado.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para operaciones relacionadas con registros de garantizado
 * Maneja la creación y gestión de registros de conductores
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@RestController
@RequestMapping("/api/registros")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    /**
     * Crea un nuevo registro de garantizado
     * @param request Datos del registro a crear
     * @return Respuesta con el registro creado o error
     */
    @PostMapping
    public ResponseEntity<RegistroResponse> crearRegistro(@Valid @RequestBody RegistroRequest request) {
        try {
            RegistroResponse response = registroService.crearRegistro(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            RegistroResponse errorResponse = new RegistroResponse();
            errorResponse.setMensaje(e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            // Manejo específico para cuando el conductor ya está registrado en la semana
            RegistroResponse errorResponse = new RegistroResponse();
            errorResponse.setMensaje(e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        } catch (Exception e) {
            RegistroResponse errorResponse = new RegistroResponse();
            errorResponse.setMensaje("Error al crear el registro: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
