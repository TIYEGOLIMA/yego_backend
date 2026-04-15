package com.yego.backend.controller.yego_marketing_mensajes;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            MarketingMensajeResponse response = marketingMensajeService.crearMensaje(request, archivo);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return buildErrorResponse("Error al crear el mensaje: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<MarketingMensajeResponse> actualizarMensaje(
            @PathVariable Long id,
            @Valid @ModelAttribute MarketingMensajeRequest request,
            @RequestParam(value = "file", required = false) MultipartFile archivo) {
        try {
            MarketingMensajeResponse response = marketingMensajeService.actualizarMensaje(id, request, archivo);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return buildErrorResponse("Error al actualizar el mensaje: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketingMensajeResponse> obtenerMensajePorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerMensajePorId(id));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Error al obtener el mensaje: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<MarketingMensajeResponse>> obtenerTodosLosMensajes() {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerTodosLosMensajes());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/activos")
    public ResponseEntity<List<MarketingMensajeResponse>> obtenerMensajesActivos() {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerMensajesActivos());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<MarketingMensajeResponse>> obtenerMensajesPorTipo(@PathVariable String tipo) {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerMensajesPorTipo(tipo));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MarketingMensajeResponse> eliminarMensaje(@PathVariable Long id) {
        try {
            marketingMensajeService.eliminarMensaje(id);
            MarketingMensajeResponse response = new MarketingMensajeResponse();
            response.setMensajeOperacion("Mensaje eliminado exitosamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return buildErrorResponse("Error al eliminar el mensaje: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/grupos")
    public ResponseEntity<List<GrupoWhatsAppResponse>> obtenerGrupos() {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerGrupos());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/flotas")
    public ResponseEntity<List<FlotaResponse>> obtenerFlotas() {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerFlotas());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<MarketingMensajeCalendarioResponse>> obtenerMensajesParaCalendario() {
        try {
            return ResponseEntity.ok(marketingMensajeService.obtenerMensajesParaCalendario());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String modo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String canales,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        try {
            byte[] excel = marketingMensajeService.exportarTodosMensajesExcel(searchTerm, modo, tipo, canales, fechaDesde, fechaHasta);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mensajes_marketing.xlsx\"");
            return ResponseEntity.ok().headers(headers).body(excel);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String modo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String canales,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        try {
            byte[] pdf = marketingMensajeService.exportarTodosMensajesPdf(searchTerm, modo, tipo, canales, fechaDesde, fechaHasta);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mensajes_marketing.pdf\"");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static ResponseEntity<MarketingMensajeResponse> buildErrorResponse(String message, HttpStatus status) {
        MarketingMensajeResponse error = new MarketingMensajeResponse();
        error.setMensajeOperacion(message);
        return new ResponseEntity<>(error, status);
    }
}
