package com.yego.backend.controller.yego_marketing_mensajes;

import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/marketing-mensajes")
public class MarketingMensajeController {

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final MarketingMensajeService marketingMensajeService;

    public MarketingMensajeController(MarketingMensajeService marketingMensajeService) {
        this.marketingMensajeService = marketingMensajeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MarketingMensajeResponse crearMensaje(
            @Valid @ModelAttribute MarketingMensajeRequest request,
            @RequestParam(value = "file", required = false) MultipartFile archivo) {
        return marketingMensajeService.crearMensaje(request, archivo);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MarketingMensajeResponse actualizarMensaje(
            @PathVariable Long id,
            @Valid @ModelAttribute MarketingMensajeRequest request,
            @RequestParam(value = "file", required = false) MultipartFile archivo) {
        return marketingMensajeService.actualizarMensaje(id, request, archivo);
    }

    @GetMapping("/{id}")
    public MarketingMensajeResponse obtenerMensajePorId(@PathVariable Long id) {
        return marketingMensajeService.obtenerMensajePorId(id);
    }

    @GetMapping
    public List<MarketingMensajeResponse> obtenerTodosLosMensajes() {
        return marketingMensajeService.obtenerTodosLosMensajes();
    }

    @GetMapping("/activos")
    public List<MarketingMensajeResponse> obtenerMensajesActivos() {
        return marketingMensajeService.obtenerMensajesActivos();
    }

    @GetMapping("/tipo/{tipo}")
    public List<MarketingMensajeResponse> obtenerMensajesPorTipo(@PathVariable String tipo) {
        return marketingMensajeService.obtenerMensajesPorTipo(tipo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarMensaje(@PathVariable Long id) {
        marketingMensajeService.eliminarMensaje(id);
    }

    @GetMapping("/grupos")
    public List<GrupoWhatsAppResponse> obtenerGrupos() {
        return marketingMensajeService.obtenerGrupos();
    }

    @GetMapping("/flotas")
    public List<FlotaResponse> obtenerFlotas() {
        return marketingMensajeService.obtenerFlotas();
    }

    @GetMapping("/calendario")
    public List<MarketingMensajeCalendarioResponse> obtenerMensajesParaCalendario() {
        return marketingMensajeService.obtenerMensajesParaCalendario();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String modo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String canales,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        byte[] content = marketingMensajeService.exportarTodosMensajesExcel(
                searchTerm, modo, tipo, canales, fechaDesde, fechaHasta);
        return download(content, "mensajes_marketing.xlsx", EXCEL_MEDIA_TYPE);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String modo,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String canales,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta) {
        byte[] content = marketingMensajeService.exportarTodosMensajesPdf(
                searchTerm, modo, tipo, canales, fechaDesde, fechaHasta);
        return download(content, "mensajes_marketing.pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> download(byte[] content, String filename, MediaType mediaType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.length)
                .body(content);
    }
}
