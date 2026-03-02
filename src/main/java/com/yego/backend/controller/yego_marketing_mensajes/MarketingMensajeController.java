package com.yego.backend.controller.yego_marketing_mensajes;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;

import java.util.List;

@RestController
@RequestMapping("/api/marketing-mensajes")
public class MarketingMensajeController {

    private final MarketingMensajeService marketingMensajeService;

    public MarketingMensajeController(MarketingMensajeService marketingMensajeService) {
        this.marketingMensajeService = marketingMensajeService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<MarketingMensajeResponse> crearMensaje(
            @Valid @ModelAttribute MarketingMensajeRequest request,
            @RequestParam(value = "file", required = false) MultipartFile archivo) {
        try {
            // Si hay un archivo, se procesará en el servicio
            MarketingMensajeResponse response = marketingMensajeService.crearMensaje(request, archivo);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion(e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion("Error al crear el mensaje: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Actualiza un mensaje de marketing existente
     * @param id ID del mensaje a actualizar
     * @param request Datos actualizados del mensaje
     * @param archivo Archivo opcional a subir
     * @return Respuesta con el mensaje actualizado o error
     */
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<MarketingMensajeResponse> actualizarMensaje(
            @PathVariable Long id,
            @Valid @ModelAttribute MarketingMensajeRequest request,
            @RequestParam(value = "file", required = false) MultipartFile archivo) {
        try {
            MarketingMensajeResponse response = marketingMensajeService.actualizarMensaje(id, request, archivo);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion(e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion("Error al actualizar el mensaje: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene un mensaje por su ID
     * @param id ID del mensaje
     * @return Respuesta con el mensaje encontrado o error
     */
    @GetMapping("/{id}")
    public ResponseEntity<MarketingMensajeResponse> obtenerMensajePorId(@PathVariable Long id) {
        try {
            MarketingMensajeResponse response = marketingMensajeService.obtenerMensajePorId(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion(e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion("Error al obtener el mensaje: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene todos los mensajes
     * @return Lista de mensajes
     */
    @GetMapping
    public ResponseEntity<List<MarketingMensajeResponse>> obtenerTodosLosMensajes() {
        try {
            List<MarketingMensajeResponse> mensajes = marketingMensajeService.obtenerTodosLosMensajes();
            return new ResponseEntity<>(mensajes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene solo los mensajes activos
     * @return Lista de mensajes activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<MarketingMensajeResponse>> obtenerMensajesActivos() {
        try {
            List<MarketingMensajeResponse> mensajes = marketingMensajeService.obtenerMensajesActivos();
            return new ResponseEntity<>(mensajes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene mensajes por tipo
     * @param tipo Tipo de mensaje
     * @return Lista de mensajes del tipo especificado
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<MarketingMensajeResponse>> obtenerMensajesPorTipo(@PathVariable String tipo) {
        try {
            List<MarketingMensajeResponse> mensajes = marketingMensajeService.obtenerMensajesPorTipo(tipo);
            return new ResponseEntity<>(mensajes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Elimina un mensaje
     * @param id ID del mensaje a eliminar
     * @return Respuesta de éxito o error
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MarketingMensajeResponse> eliminarMensaje(@PathVariable Long id) {
        try {
            marketingMensajeService.eliminarMensaje(id);
            MarketingMensajeResponse response = new MarketingMensajeResponse();
            response.setMensajeOperacion("Mensaje eliminado exitosamente");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion(e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            MarketingMensajeResponse errorResponse = new MarketingMensajeResponse();
            errorResponse.setMensajeOperacion("Error al eliminar el mensaje: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene el histórico de todas las acciones
     * @return Lista del histórico
     */
    @GetMapping("/historico")
    public ResponseEntity<?> obtenerHistorico() {
        try {
            return new ResponseEntity<>(marketingMensajeService.obtenerHistorico(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene el histórico de un mensaje específico
     * @param mensajeId ID del mensaje
     * @return Lista del histórico del mensaje
     */
    @GetMapping("/historico/{mensajeId}")
    public ResponseEntity<?> obtenerHistoricoPorMensajeId(@PathVariable Long mensajeId) {
        try {
            return new ResponseEntity<>(marketingMensajeService.obtenerHistoricoPorMensajeId(mensajeId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene los grupos disponibles de WhatsApp desde la base de datos
     * @return Lista de grupos con id, subject y pictureUrl
     */
    @GetMapping("/grupos")
    public ResponseEntity<List<GrupoWhatsAppResponse>> obtenerGrupos() {
        try {
            List<GrupoWhatsAppResponse> grupos = marketingMensajeService.obtenerGrupos();
            return new ResponseEntity<>(grupos, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene las flotas disponibles
     * @return Lista de flotas
     */
    @GetMapping("/flotas")
    public ResponseEntity<List<FlotaResponse>> obtenerFlotas() {
        try {
            List<FlotaResponse> flotas = marketingMensajeService.obtenerFlotas();
            return new ResponseEntity<>(flotas, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene todos los mensajes para el calendario
     * Solo incluye los campos necesarios para el calendario: id, titulo, diasActivos, horaInicio, horaFin
     * @return Lista de mensajes para el calendario
     */
    @GetMapping("/calendario")
    public ResponseEntity<List<MarketingMensajeCalendarioResponse>> obtenerMensajesParaCalendario() {
        try {
            List<MarketingMensajeCalendarioResponse> mensajes = marketingMensajeService.obtenerMensajesParaCalendario();
            return new ResponseEntity<>(mensajes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Exporta mensajes a Excel. Acepta los mismos filtros que la lista (searchTerm, modo, tipo, canales).
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String modo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String canales) {
        try {
            byte[] excel = marketingMensajeService.exportarTodosMensajesExcel(searchTerm, modo, tipo, canales);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mensajes_marketing.xlsx\"");
            return ResponseEntity.ok().headers(headers).body(excel);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Exporta mensajes a PDF. Acepta los mismos filtros que la lista.
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String modo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String canales) {
        try {
            byte[] pdf = marketingMensajeService.exportarTodosMensajesPdf(searchTerm, modo, tipo, canales);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mensajes_marketing.pdf\"");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

