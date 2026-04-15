package com.yego.backend.service.yego_marketing_mensajes.impl;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final RestTemplate restTemplate;
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
                                      RestTemplate restTemplate, 
                                      MinIOService minIOService) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.moduleMarketingGroupRepository = moduleMarketingGroupRepository;
        this.restTemplate = restTemplate;
        this.minIOService = minIOService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public MarketingMensajeResponse crearMensaje(MarketingMensajeRequest request, MultipartFile archivo) {
        log.info("Creando nuevo mensaje: {}", request.getTitulo());

        Long userId = obtenerUserIdActual();

        String urlArchivo = subirArchivoSiPresente(archivo);

        MarketingMensaje mensaje = new MarketingMensaje();
        mensaje.setUserId(userId);
        mensaje.setTitulo(request.getTitulo());
        mensaje.setMensaje(request.getMensaje());
        mensaje.setModo(request.getModo());
        mensaje.setTipo(request.getTipo());
        mensaje.setArchivo(urlArchivo != null ? urlArchivo : request.getArchivo());
        mensaje.setWhatsapp(request.getWhatsapp());
        mensaje.setYandex(request.getYandex());
        mensaje.setDiasActivos(convertirListaAJson(request.getDiasActivos()));
        mensaje.setGrupos(convertirListaAJson(request.getGrupos()));
        mensaje.setFlotas(convertirListaAJson(request.getFlotas()));
        guardarHorasEspecificas(mensaje, request.getHorasEspecificas());
        mensaje.setActivo(request.getActivo() != null ? request.getActivo() : true);

        MarketingMensaje mensajeGuardado = marketingMensajeRepository.save(mensaje);
        log.info("Mensaje creado con ID: {}", mensajeGuardado.getId());

        return convertirAResponse(mensajeGuardado, "Mensaje creado exitosamente");
    }

    @Override
    @Transactional
    public MarketingMensajeResponse actualizarMensaje(Long id, MarketingMensajeRequest request, MultipartFile archivo) {
        log.info("Actualizando mensaje con ID: {}", id);

        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensaje con ID " + id + " no encontrado"));

        String tipoAnterior = mensaje.getTipo();

        if (archivo != null && !archivo.isEmpty()) {
            String nuevaUrlArchivo = subirArchivoSiPresente(archivo);
            if (nuevaUrlArchivo != null) {
                mensaje.setArchivo(nuevaUrlArchivo);
            }
        } else if (request.getArchivo() != null) {
            mensaje.setArchivo(request.getArchivo());
        }

        if (request.getTitulo() != null) mensaje.setTitulo(request.getTitulo());
        if (request.getMensaje() != null) mensaje.setMensaje(request.getMensaje());
        if (request.getModo() != null) mensaje.setModo(request.getModo());

        if (request.getTipo() != null) {
            String tipoNuevo = request.getTipo();
            if ("ninguna".equalsIgnoreCase(tipoNuevo.trim()) && 
                (tipoAnterior == null || !"ninguna".equalsIgnoreCase(tipoAnterior.trim()))) {
                eliminarArchivoAsociado(mensaje);
            }
            mensaje.setTipo(tipoNuevo);
        }

        if (request.getWhatsapp() != null) mensaje.setWhatsapp(request.getWhatsapp());
        if (request.getYandex() != null) mensaje.setYandex(request.getYandex());

        if (request.getDiasActivos() != null) {
            mensaje.setDiasActivos(convertirListaAJson(limpiarListaDeJsonStrings(request.getDiasActivos())));
        }
        if (request.getGrupos() != null) {
            mensaje.setGrupos(convertirListaAJson(limpiarListaDeJsonStrings(request.getGrupos())));
        }
        if (request.getFlotas() != null) {
            mensaje.setFlotas(convertirListaAJson(limpiarListaDeJsonStrings(request.getFlotas())));
        }

        guardarHorasEspecificas(mensaje, request.getHorasEspecificas());

        if (request.getActivo() != null) mensaje.setActivo(request.getActivo());

        MarketingMensaje mensajeActualizado = marketingMensajeRepository.save(mensaje);
        log.info("Mensaje actualizado con ID: {}", mensajeActualizado.getId());

        return convertirAResponse(mensajeActualizado, "Mensaje actualizado exitosamente");
    }

    @Override
    public MarketingMensajeResponse obtenerMensajePorId(Long id) {
        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensaje con ID " + id + " no encontrado"));
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
        if (!marketingMensajeRepository.existsById(id)) {
            throw new IllegalArgumentException("Mensaje con ID " + id + " no encontrado");
        }
        marketingMensajeRepository.deleteById(id);
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
            try {
                List<GrupoWhatsAppResponse> grupos = moduleMarketingGroupRepository.findAll().stream()
                        .map(this::mapGrupoToResponse)
                        .collect(Collectors.toList());
                gruposCache = grupos;
                gruposCacheExpiry = now + CACHE_TTL_MS;
                log.info("Grupos de WhatsApp cargados: {} (caché 5 min)", grupos.size());
                return grupos;
            } catch (Exception e) {
                log.error("Error obteniendo grupos de WhatsApp: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
            try {
                String url = "http://162.55.214.109:6000/v2/partners";
                Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
                List<FlotaResponse> flotas = new ArrayList<>();
                if (response != null && response.containsKey("partners")) {
                    List<Map<String, Object>> partners = (List<Map<String, Object>>) response.get("partners");
                    for (Map<String, Object> item : partners) {
                        FlotaResponse flota = new FlotaResponse();
                        flota.setId(item.get("id").toString());
                        flota.setName(item.get("name").toString());
                        flota.setCity(item.get("city") != null ? item.get("city").toString() : null);
                        flota.setSpecifications((List<String>) item.get("specifications"));
                        flotas.add(flota);
                    }
                }
                flotasCache = flotas;
                flotasCacheExpiry = now + CACHE_TTL_MS;
                log.info("Flotas cargadas: {} (caché 5 min)", flotas.size());
                return flotas;
            } catch (Exception e) {
                log.error("Error obteniendo flotas: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
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
        } catch (Exception e) {
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
            document.add(new Paragraph("Listado de mensajes - SMS Marketing YEGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
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
        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando PDF de mensajes", e);
        }
    }

    // ── Métodos privados auxiliares ──────────────────────────────────────

    private String subirArchivoSiPresente(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) return null;
        log.info("Subiendo archivo a MinIO: {}", archivo.getOriginalFilename());
        String url = minIOService.subirArchivo(archivo);
        if (url != null) {
            log.info("Archivo subido: {}", url);
        } else {
            log.warn("No se pudo subir el archivo a MinIO");
        }
        return url;
    }

    private void eliminarArchivoAsociado(MarketingMensaje mensaje) {
        String archivoActual = mensaje.getArchivo();
        if (archivoActual == null || archivoActual.isEmpty()) return;
        log.info("Tipo cambió a 'ninguna', eliminando archivo: {}", archivoActual);
        try {
            minIOService.eliminarArchivo(archivoActual);
        } catch (Exception e) {
            log.error("Error eliminando archivo de MinIO: {}", e.getMessage(), e);
        }
        mensaje.setArchivo(null);
    }

    private void guardarHorasEspecificas(MarketingMensaje mensaje, String horasEspecificas) {
        if (horasEspecificas == null || horasEspecificas.trim().isEmpty()) return;
        try {
            objectMapper.readTree(horasEspecificas);
        } catch (Exception e) {
            log.warn("Horas específicas no es JSON válido: {}", e.getMessage());
        }
        mensaje.setHorasEspecificas(horasEspecificas);
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
        } catch (Exception e) {
            log.error("Error obteniendo userId del contexto de seguridad: {}", e.getMessage());
        }
        return null;
    }

    // ── JSON <-> List helpers ───────────────────────────────────────────

    private String convertirListaAJson(List<String> lista) {
        if (lista == null || lista.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (Exception e) {
            log.error("Error convirtiendo lista a JSON: {}", e.getMessage());
            return null;
        }
    }

    private List<String> convertirJsonALista(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        String jsonLimpio = limpiarJsonString(json);
        try {
            List<String> resultado = objectMapper.readValue(jsonLimpio, new TypeReference<List<String>>() {});
            List<String> resultadoLimpio = new ArrayList<>();
            for (String item : resultado) {
                if (item != null && item.trim().startsWith("[") && item.trim().endsWith("]")) {
                    resultadoLimpio.addAll(convertirJsonALista(item));
                } else {
                    resultadoLimpio.add(item);
                }
            }
            return resultadoLimpio;
        } catch (Exception e) {
            log.warn("Error convirtiendo JSON a lista: {} | JSON: {}", 
                e.getMessage(), json.substring(0, Math.min(100, json.length())));
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private String limpiarJsonString(String json) {
        if (json == null || json.trim().isEmpty()) return json;
        String jsonLimpio = json.trim();
        for (int i = 0; i < 5; i++) {
            try {
                Object parsed = objectMapper.readValue(jsonLimpio, Object.class);
                if (parsed instanceof List) {
                    List<Object> lista = (List<Object>) parsed;
                    if (lista.size() == 1 && lista.get(0) instanceof String) {
                        String elemento = (String) lista.get(0);
                        if ((elemento.trim().startsWith("[") && elemento.trim().endsWith("]")) ||
                            (elemento.trim().startsWith("{") && elemento.trim().endsWith("}"))) {
                            jsonLimpio = elemento;
                            continue;
                        }
                    }
                }
                break;
            } catch (Exception e) {
                break;
            }
        }
        return jsonLimpio;
    }

    @SuppressWarnings("unchecked")
    private List<String> limpiarListaDeJsonStrings(List<String> lista) {
        if (lista == null || lista.isEmpty()) return Collections.emptyList();
        List<String> resultado = new ArrayList<>();
        for (String item : lista) {
            if (item == null || item.trim().isEmpty()) continue;
            String trimmed = item.trim();
            if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
                try {
                    Object parsed = objectMapper.readValue(trimmed, Object.class);
                    if (parsed instanceof List) {
                        List<Object> parsedList = (List<Object>) parsed;
                        if (parsedList.isEmpty()) continue;
                        for (Object element : parsedList) {
                            if (element != null) {
                                String str = element.toString();
                                if (str.trim().startsWith("[") && str.trim().endsWith("]")) {
                                    resultado.addAll(limpiarListaDeJsonStrings(Collections.singletonList(str)));
                                } else {
                                    resultado.add(str);
                                }
                            }
                        }
                    } else {
                        resultado.add(item);
                    }
                } catch (Exception e) {
                    resultado.add(item);
                }
            } else {
                resultado.add(item);
            }
        }
        return resultado;
    }

    // ── Export helpers ───────────────────────────────────────────────────

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
            try {
                LocalDate desde = LocalDate.parse(fechaDesde.trim());
                list = list.stream().filter(m -> 
                    m.getCreatedAt() != null && !m.getCreatedAt().toLocalDate().isBefore(desde)
                ).collect(Collectors.toList());
            } catch (Exception ignored) { }
        }
        if (fechaHasta != null && !fechaHasta.trim().isEmpty()) {
            try {
                LocalDate hasta = LocalDate.parse(fechaHasta.trim());
                list = list.stream().filter(m -> 
                    m.getCreatedAt() != null && !m.getCreatedAt().toLocalDate().isAfter(hasta)
                ).collect(Collectors.toList());
            } catch (Exception ignored) { }
        }
        return list;
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
