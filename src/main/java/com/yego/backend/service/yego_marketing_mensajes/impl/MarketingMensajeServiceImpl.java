package com.yego.backend.service.yego_marketing_mensajes.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.entity.yego_marketing_mensajes.entities.ModuleMarketingGroup;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.repository.yego_marketing_mensajes.ModuleMarketingGroupRepository;
import com.yego.backend.service.MinIOService;
import com.yego.backend.service.yego_garantizado.FlotaService;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketingMensajeServiceImpl implements MarketingMensajeService {

    private final MarketingMensajeRepository marketingMensajeRepository;
    private final ModuleMarketingGroupRepository moduleMarketingGroupRepository;
    private final FlotaService flotaService;
    private final ObjectMapper objectMapper;
    private final MinIOService minIOService;

    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private volatile List<FlotaResponse> flotasCache;
    private volatile long flotasCacheExpiry;
    private volatile List<GrupoWhatsAppResponse> gruposCache;
    private volatile long gruposCacheExpiry;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, String> MAPA_DIAS_NORMALIZADOS;
    static {
        MAPA_DIAS_NORMALIZADOS = Map.ofEntries(
            Map.entry("lunes", "Lun"), Map.entry("martes", "Mar"),
            Map.entry("miercoles", "Mié"), Map.entry("miércoles", "Mié"),
            Map.entry("jueves", "Jue"), Map.entry("viernes", "Vie"),
            Map.entry("sabado", "Sáb"), Map.entry("sábado", "Sáb"),
            Map.entry("domingo", "Dom"),
            Map.entry("lun", "Lun"), Map.entry("mar", "Mar"),
            Map.entry("mié", "Mié"), Map.entry("jue", "Jue"),
            Map.entry("vie", "Vie"), Map.entry("sáb", "Sáb"),
            Map.entry("dom", "Dom"),
            Map.entry("monday", "Lun"), Map.entry("tuesday", "Mar"),
            Map.entry("wednesday", "Mié"), Map.entry("thursday", "Jue"),
            Map.entry("friday", "Vie"), Map.entry("saturday", "Sáb"),
            Map.entry("sunday", "Dom")
        );
    }

    public MarketingMensajeServiceImpl(MarketingMensajeRepository marketingMensajeRepository, 
                                      ModuleMarketingGroupRepository moduleMarketingGroupRepository,
                                      FlotaService flotaService,
                                      MinIOService minIOService,
                                      ObjectMapper objectMapper) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.moduleMarketingGroupRepository = moduleMarketingGroupRepository;
        this.flotaService = flotaService;
        this.minIOService = minIOService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MarketingMensajeResponse crearMensaje(MarketingMensajeRequest request, MultipartFile archivo) {
        log.info("Creando nuevo mensaje: {}", request.getTitulo());

        Long userId = obtenerUserIdActual();

        MarketingMensaje mensaje = new MarketingMensaje();
        mensaje.setUserId(userId);
        mensaje.setTitulo(request.getTitulo());
        mensaje.setMensaje(request.getMensaje());
        mensaje.setModo(request.getModo());
        mensaje.setTipo(request.getTipo());
        mensaje.setArchivo(request.getArchivo());
        mensaje.setDiasActivos(convertirListaAJson(normalizarLista(request.getDiasActivos())));
        mensaje.setGrupos(convertirListaAJson(normalizarLista(request.getGrupos())));
        mensaje.setFlotas(convertirListaAJson(normalizarLista(request.getFlotas())));
        mensaje.setWhatsapp(Boolean.TRUE.equals(request.getWhatsapp()));
        mensaje.setYandex(Boolean.TRUE.equals(request.getYandex()));
        activarCanalesConDestinatarios(mensaje);
        guardarHorasEspecificas(mensaje, request.getHorasEspecificas());
        mensaje.setActivo(request.getActivo() != null ? request.getActivo() : true);
        validarCanales(mensaje);

        String urlArchivo = subirArchivoSiPresente(archivo);
        if (urlArchivo != null) {
            mensaje.setArchivo(urlArchivo);
        }

        MarketingMensaje mensajeGuardado = marketingMensajeRepository.saveAndFlush(mensaje);
        log.info("Mensaje creado con ID: {}", mensajeGuardado.getId());

        return convertirAResponse(mensajeGuardado, "Mensaje creado exitosamente");
    }

    @Override
    @Transactional
    public MarketingMensajeResponse actualizarMensaje(Long id, MarketingMensajeRequest request, MultipartFile archivo) {
        log.info("Actualizando mensaje con ID: {}", id);

        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> notFound(id));

        String tipoAnterior = mensaje.getTipo();
        String archivoAnterior = mensaje.getArchivo();
        boolean eliminarArchivoAnterior = false;

        if ((archivo == null || archivo.isEmpty()) && request.getArchivo() != null) {
            mensaje.setArchivo(request.getArchivo());
        }

        if (request.getTitulo() != null) mensaje.setTitulo(request.getTitulo());
        if (request.getMensaje() != null) mensaje.setMensaje(request.getMensaje());
        if (request.getModo() != null) mensaje.setModo(request.getModo());

        if (request.getTipo() != null) {
            String tipoNuevo = request.getTipo();
            if ("ninguna".equalsIgnoreCase(tipoNuevo.trim()) && 
                (tipoAnterior == null || !"ninguna".equalsIgnoreCase(tipoAnterior.trim()))) {
                mensaje.setArchivo(null);
                eliminarArchivoAnterior = true;
            }
            mensaje.setTipo(tipoNuevo);
        }

        if (request.getWhatsapp() != null) mensaje.setWhatsapp(request.getWhatsapp());
        if (request.getYandex() != null) mensaje.setYandex(request.getYandex());

        if (request.getDiasActivos() != null) {
            mensaje.setDiasActivos(convertirListaAJson(normalizarLista(request.getDiasActivos())));
        }
        if (request.getGrupos() != null) {
            mensaje.setGrupos(convertirListaAJson(normalizarLista(request.getGrupos())));
        }
        if (request.getFlotas() != null) {
            mensaje.setFlotas(convertirListaAJson(normalizarLista(request.getFlotas())));
        }
        activarCanalesConDestinatarios(mensaje);

        guardarHorasEspecificas(mensaje, request.getHorasEspecificas());

        if (request.getActivo() != null) mensaje.setActivo(request.getActivo());
        validarCanales(mensaje);

        String nuevaUrlArchivo = subirArchivoSiPresente(archivo);
        if (nuevaUrlArchivo != null) {
            mensaje.setArchivo(nuevaUrlArchivo);
            eliminarArchivoAnterior = archivoAnterior != null
                    && !archivoAnterior.equals(nuevaUrlArchivo);
        }

        MarketingMensaje mensajeActualizado = marketingMensajeRepository.saveAndFlush(mensaje);
        if (eliminarArchivoAnterior) {
            eliminarArchivo(archivoAnterior);
        }
        log.info("Mensaje actualizado con ID: {}", mensajeActualizado.getId());

        return convertirAResponse(mensajeActualizado, "Mensaje actualizado exitosamente");
    }

    @Override
    public MarketingMensajeResponse obtenerMensajePorId(Long id) {
        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        return convertirAResponse(mensaje, null);
    }

    @Override
    public List<MarketingMensajeResponse> obtenerTodosLosMensajes() {
        return marketingMensajeRepository.findAll().stream()
                .map(m -> convertirAResponse(m, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketingMensajeResponse> obtenerMensajesActivos() {
        return marketingMensajeRepository.findByActivoTrue().stream()
                .map(m -> convertirAResponse(m, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketingMensajeResponse> obtenerMensajesPorTipo(String tipo) {
        return marketingMensajeRepository.findByTipoAndActivoTrue(tipo).stream()
                .map(m -> convertirAResponse(m, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarMensaje(Long id) {
        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        String archivo = mensaje.getArchivo();
        marketingMensajeRepository.delete(mensaje);
        marketingMensajeRepository.flush();
        eliminarArchivo(archivo);
        log.info("Mensaje eliminado con ID: {}", id);
    }

    @Override
    public List<GrupoWhatsAppResponse> obtenerGrupos() {
        long now = System.currentTimeMillis();
        if (gruposCache != null && now < gruposCacheExpiry) {
            return gruposCache;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (gruposCache != null && now < gruposCacheExpiry) {
                return gruposCache;
            }
            List<GrupoWhatsAppResponse> grupos = moduleMarketingGroupRepository.findAll().stream()
                    .map(this::mapGrupoToResponse)
                    .collect(Collectors.toList());
            gruposCache = grupos;
            gruposCacheExpiry = now + CACHE_TTL_MS;
            log.info("Grupos de WhatsApp cargados: {} (caché 5 min)", grupos.size());
            return grupos;
        }
    }

    @Override
    public List<FlotaResponse> obtenerFlotas() {
        long now = System.currentTimeMillis();
        if (flotasCache != null && now < flotasCacheExpiry) {
            return flotasCache;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (flotasCache != null && now < flotasCacheExpiry) {
                return flotasCache;
            }
            List<FlotaResponse> flotas = List.copyOf(flotaService.obtenerTodosLosPartners());
            flotasCache = flotas;
            flotasCacheExpiry = now + CACHE_TTL_MS;
            log.info("Flotas cargadas: {} (caché 5 min)", flotas.size());
            return flotas;
        }
    }

    @Override
    public List<MarketingMensajeCalendarioResponse> obtenerMensajesParaCalendario() {
        return marketingMensajeRepository.findAll().stream()
                .map(this::convertirACalendarioResponse)
                .collect(Collectors.toList());
    }

    // ── Exportación ─────────────────────────────────────────────────────

    @Override
    public byte[] exportarTodosMensajesExcel(String searchTerm, String modo, String tipo, String canales, String fechaDesde, String fechaHasta) {
        List<MarketingMensajeResponse> mensajes = filtrarYOrdenarParaExport(searchTerm, modo, tipo, canales, fechaDesde, fechaHasta);
        Map<String, String> grupoIdToName = obtenerGrupos().stream()
                .collect(Collectors.toMap(GrupoWhatsAppResponse::getId, g -> g.getSubject() != null ? g.getSubject() : g.getId(), (a, b) -> a));
        Map<String, String> flotaIdToName = obtenerFlotas().stream()
                .collect(Collectors.toMap(FlotaResponse::getId, f -> f.getName() != null ? f.getName() : f.getId(), (a, b) -> a));
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mensajes Marketing");
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setWrapText(true);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setWrapText(true);

            String[] headers = { "ID", "Título", "Mensaje", "Modo", "Tipo", "WhatsApp", "Yandex", "Días activos", "Grupos", "Flotas", "Horas específicas", "Activo", "Creado", "Actualizado" };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (MarketingMensajeResponse m : mensajes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getId() != null ? m.getId() : 0);
                setCell(row, 1, m.getTitulo(), dataStyle);
                setCell(row, 2, m.getMensaje(), dataStyle);
                setCell(row, 3, m.getModo(), dataStyle);
                setCell(row, 4, m.getTipo(), dataStyle);
                setCell(row, 5, Boolean.TRUE.equals(m.getWhatsapp()) ? "Sí" : "No", dataStyle);
                setCell(row, 6, Boolean.TRUE.equals(m.getYandex()) ? "Sí" : "No", dataStyle);
                setCell(row, 7, m.getDiasActivos() != null ? String.join(", ", m.getDiasActivos()) : "", dataStyle);
                setCell(row, 8, resolverNombres(m.getGrupos(), grupoIdToName), dataStyle);
                setCell(row, 9, resolverNombres(m.getFlotas(), flotaIdToName), dataStyle);
                setCell(row, 10, m.getHorasEspecificas() != null ? m.getHorasEspecificas() : "", dataStyle);
                setCell(row, 11, Boolean.TRUE.equals(m.getActivo()) ? "Sí" : "No", dataStyle);
                setCell(row, 12, m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FORMATTER) : "", dataStyle);
                setCell(row, 13, m.getUpdatedAt() != null ? m.getUpdatedAt().format(DATE_FORMATTER) : "", dataStyle);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Excel generado con {} mensajes", mensajes.size());
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error generando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando Excel de mensajes", e);
        }
    }

    @Override
    public byte[] exportarTodosMensajesPdf(String searchTerm, String modo, String tipo, String canales, String fechaDesde, String fechaHasta) {
        List<MarketingMensajeResponse> mensajes = filtrarYOrdenarParaExport(searchTerm, modo, tipo, canales, fechaDesde, fechaHasta);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Listado de mensajes - Yego Marketing", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{1f, 2f, 3f, 1f, 1f, 2f, 2f, 2f});
            for (String h : new String[]{"ID", "Título", "Mensaje", "Modo", "Tipo", "Días", "Canales", "Creado"}) {
                table.addCell(crearCeldaHeader(h));
            }

            for (MarketingMensajeResponse m : mensajes) {
                table.addCell(crearCelda(m.getId() != null ? m.getId().toString() : ""));
                table.addCell(crearCelda(m.getTitulo()));
                table.addCell(crearCelda(truncar(m.getMensaje(), 80)));
                table.addCell(crearCelda(m.getModo()));
                table.addCell(crearCelda(m.getTipo()));
                table.addCell(crearCelda(m.getDiasActivos() != null ? String.join(", ", m.getDiasActivos()) : ""));
                String textoCanales = buildCanalesText(m);
                table.addCell(crearCelda(textoCanales));
                table.addCell(crearCelda(m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FORMATTER) : ""));
            }

            document.add(table);
            document.close();
            log.info("PDF generado con {} mensajes", mensajes.size());
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando PDF de mensajes", e);
        }
    }

    @Override
    public void invalidarCacheGrupos() {
        this.gruposCache = null;
        this.gruposCacheExpiry = 0;
        log.info("Cache de grupos de Marketing invalidada");
    }

    private String subirArchivoSiPresente(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) return null;
        log.info("Subiendo archivo a MinIO: {}", archivo.getOriginalFilename());
        String url = minIOService.subirArchivo(archivo);
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "No se pudo almacenar el archivo adjunto");
        }
        log.info("Archivo subido correctamente");
        return url;
    }

    private void eliminarArchivo(String archivo) {
        if (archivo == null || archivo.isBlank()) return;
        try {
            if (!minIOService.eliminarArchivo(archivo)) {
                log.warn("No se pudo eliminar un archivo anterior de Marketing");
            }
        } catch (Exception e) {
            log.warn("No se pudo eliminar un archivo anterior de Marketing ({})",
                    e.getClass().getSimpleName());
        }
    }

    private void guardarHorasEspecificas(MarketingMensaje mensaje, String horasEspecificas) {
        if (horasEspecificas == null || horasEspecificas.trim().isEmpty()) return;
        try {
            JsonNode value = objectMapper.readTree(horasEspecificas);
            if (!value.isObject()) {
                throw badRequest("Las horas específicas deben ser un objeto JSON");
            }
        } catch (JsonProcessingException e) {
            throw badRequest("Las horas específicas no contienen JSON válido");
        }
        mensaje.setHorasEspecificas(horasEspecificas);
    }

    private void validarCanales(MarketingMensaje mensaje) {
        if (!Boolean.TRUE.equals(mensaje.getActivo())) {
            return;
        }

        boolean whatsapp = Boolean.TRUE.equals(mensaje.getWhatsapp());
        boolean fleet = Boolean.TRUE.equals(mensaje.getYandex());
        if (!whatsapp && !fleet) {
            throw badRequest("Una campaña activa debe tener al menos un canal");
        }
        if (whatsapp && convertirJsonALista(mensaje.getGrupos()).isEmpty()) {
            throw badRequest("Selecciona al menos un grupo para WhatsApp");
        }
        if (fleet && convertirJsonALista(mensaje.getFlotas()).isEmpty()) {
            throw badRequest("Selecciona al menos una flota para Fleet");
        }
    }

    private void activarCanalesConDestinatarios(MarketingMensaje mensaje) {
        if (!convertirJsonALista(mensaje.getGrupos()).isEmpty()) {
            mensaje.setWhatsapp(true);
        }
        if (!convertirJsonALista(mensaje.getFlotas()).isEmpty()) {
            mensaje.setYandex(true);
        }
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Mensaje con ID " + id + " no encontrado");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private GrupoWhatsAppResponse mapGrupoToResponse(ModuleMarketingGroup grupo) {
        GrupoWhatsAppResponse response = new GrupoWhatsAppResponse();
        response.setId(grupo.getGroupId());
        response.setSubject(grupo.getSubject());
        response.setPictureUrl(grupo.getPictureUrl());
        return response;
    }

    private MarketingMensajeCalendarioResponse convertirACalendarioResponse(MarketingMensaje entity) {
        MarketingMensajeCalendarioResponse response = new MarketingMensajeCalendarioResponse();
        response.setId(entity.getId());
        response.setTitulo(entity.getTitulo());
        response.setModo(entity.getModo());
        response.setDiasActivos(normalizarDiasActivos(convertirJsonALista(entity.getDiasActivos())));
        response.setHorasEspecificas(entity.getHorasEspecificas());
        return response;
    }

    private MarketingMensajeResponse convertirAResponse(MarketingMensaje entity, String mensajeOperacion) {
        MarketingMensajeResponse response = new MarketingMensajeResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setTitulo(entity.getTitulo());
        response.setMensaje(entity.getMensaje());
        response.setModo(entity.getModo());
        response.setTipo(entity.getTipo());
        response.setArchivo(entity.getArchivo());
        response.setWhatsapp(entity.getWhatsapp());
        response.setYandex(entity.getYandex());
        response.setActivo(entity.getActivo());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setMensajeOperacion(mensajeOperacion);
        response.setDiasActivos(normalizarDiasActivos(convertirJsonALista(entity.getDiasActivos())));
        response.setGrupos(convertirJsonALista(entity.getGrupos()));
        response.setFlotas(convertirJsonALista(entity.getFlotas()));
        response.setHorasEspecificas(entity.getHorasEspecificas());
        return response;
    }

    private List<String> normalizarDiasActivos(List<String> dias) {
        if (dias == null || dias.isEmpty()) return Collections.emptyList();
        return dias.stream()
                .map(dia -> {
                    if (dia == null) return null;
                    return MAPA_DIAS_NORMALIZADOS.getOrDefault(dia.toLowerCase().trim(), dia);
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long obtenerUserIdActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                return Long.parseLong(auth.getName());
            }
        } catch (NumberFormatException e) {
            log.warn("El identificador del usuario autenticado no es numérico: {}", e.getMessage());
        }
        return null;
    }

    private String convertirListaAJson(List<String> lista) {
        if (lista == null || lista.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la lista de Marketing", e);
        }
    }

    private List<String> convertirJsonALista(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            return normalizarLista(objectMapper.readValue(
                    json, new TypeReference<List<String>>() {}));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Datos JSON inválidos en Marketing", e);
        }
    }

    private List<String> normalizarLista(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private List<MarketingMensajeResponse> filtrarYOrdenarParaExport(
            String searchTerm, String modo, String tipo, String canales, String fechaDesde, String fechaHasta) {
        List<MarketingMensajeResponse> list = obtenerTodosLosMensajes();

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String q = searchTerm.trim().toLowerCase();
            list = list.stream().filter(m ->
                contains(m.getTitulo(), q) || contains(m.getMensaje(), q) ||
                contains(m.getModo(), q) || contains(m.getTipo(), q)
            ).collect(Collectors.toList());
        }
        if (modo != null && !modo.isEmpty() && !"all".equalsIgnoreCase(modo)) {
            list = list.stream().filter(m -> modo.equals(m.getModo())).collect(Collectors.toList());
        }
        if (tipo != null && !tipo.isEmpty() && !"all".equalsIgnoreCase(tipo)) {
            list = list.stream().filter(m -> tipo.equals(m.getTipo())).collect(Collectors.toList());
        }
        if (canales != null && !canales.isEmpty() && !"all".equalsIgnoreCase(canales)) {
            list = list.stream().filter(m -> filtrarPorCanal(m, canales)).collect(Collectors.toList());
        }
        list = filtrarPorRangoFechas(list, fechaDesde, fechaHasta);

        list.sort(Comparator.comparing(
            (MarketingMensajeResponse m) -> m.getCreatedAt() != null ? m.getCreatedAt() : java.time.LocalDateTime.MIN
        ).reversed());

        return list;
    }

    private boolean filtrarPorCanal(MarketingMensajeResponse m, String canales) {
        return switch (canales.toLowerCase()) {
            case "whatsapp" -> Boolean.TRUE.equals(m.getWhatsapp());
            case "yandex" -> Boolean.TRUE.equals(m.getYandex());
            case "ambos" -> Boolean.TRUE.equals(m.getWhatsapp()) && Boolean.TRUE.equals(m.getYandex());
            default -> true;
        };
    }

    private List<MarketingMensajeResponse> filtrarPorRangoFechas(
            List<MarketingMensajeResponse> list, String fechaDesde, String fechaHasta) {
        if (fechaDesde != null && !fechaDesde.trim().isEmpty()) {
            LocalDate desde = parseDate(fechaDesde, "fechaDesde");
            list = list.stream().filter(m ->
                m.getCreatedAt() != null && !m.getCreatedAt().toLocalDate().isBefore(desde)
            ).collect(Collectors.toList());
        }
        if (fechaHasta != null && !fechaHasta.trim().isEmpty()) {
            LocalDate hasta = parseDate(fechaHasta, "fechaHasta");
            list = list.stream().filter(m ->
                m.getCreatedAt() != null && !m.getCreatedAt().toLocalDate().isAfter(hasta)
            ).collect(Collectors.toList());
        }
        return list;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw badRequest(field + " debe tener formato yyyy-MM-dd");
        }
    }

    private static boolean contains(String text, String query) {
        return text != null && text.toLowerCase().contains(query);
    }

    private static String resolverNombres(List<String> ids, Map<String, String> idToName) {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(id -> idToName.getOrDefault(id, id)).collect(Collectors.joining(", "));
    }

    private static String buildCanalesText(MarketingMensajeResponse m) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(m.getWhatsapp())) sb.append("WhatsApp");
        if (Boolean.TRUE.equals(m.getYandex())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Yandex");
        }
        return sb.toString();
    }

    private static void setCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static PdfPCell crearCeldaHeader(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell crearCelda(String text) {
        return new PdfPCell(new Phrase(text != null ? text : "", FontFactory.getFont(FontFactory.HELVETICA, 7)));
    }

    private static String truncar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
