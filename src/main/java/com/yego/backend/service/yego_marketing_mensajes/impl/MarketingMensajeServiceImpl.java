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
import com.yego.backend.service.WhatsAppService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio para operaciones relacionadas con mensajes de marketing
 * Maneja la creación, actualización y gestión de mensajes
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Service
@Slf4j
public class MarketingMensajeServiceImpl implements MarketingMensajeService {

    private final MarketingMensajeRepository marketingMensajeRepository;
    private final ModuleMarketingGroupRepository moduleMarketingGroupRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MinIOService minIOService;

    /** Caché en memoria para flotas (API externa lenta). TTL 5 minutos. */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private volatile List<FlotaResponse> flotasCache;
    private volatile long flotasCacheExpiry;
    private volatile List<GrupoWhatsAppResponse> gruposCache;
    private volatile long gruposCacheExpiry;

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
        log.info("📝 [MarketingMensajeService] Creando nuevo mensaje: {}", request.getTitulo());

        // Obtener el userId del contexto de seguridad
        Long userId = obtenerUserIdActual();
        log.info("👤 [MarketingMensajeService] Creando mensaje para usuario ID: {}", userId);

        // Si hay un archivo, subirlo a MinIO
        String urlArchivo = null;
        if (archivo != null && !archivo.isEmpty()) {
            log.info("📤 [MarketingMensajeService] Subiendo archivo a MinIO: {}", archivo.getOriginalFilename());
            urlArchivo = minIOService.subirArchivo(archivo);
            if (urlArchivo != null) {
                log.info("✅ [MarketingMensajeService] Archivo subido exitosamente: {}", urlArchivo);
            } else {
                log.warn("⚠️ [MarketingMensajeService] No se pudo subir el archivo a MinIO");
            }
        }

        MarketingMensaje mensaje = new MarketingMensaje();
        mensaje.setUserId(userId);
        mensaje.setTitulo(request.getTitulo());
        mensaje.setMensaje(request.getMensaje());
        mensaje.setModo(request.getModo());
        mensaje.setTipo(request.getTipo());
        // Usar la URL del archivo subido si existe, sino usar el valor del request
        mensaje.setArchivo(urlArchivo != null ? urlArchivo : request.getArchivo());
        mensaje.setWhatsapp(request.getWhatsapp());
        mensaje.setYandex(request.getYandex());
        
        // Convertir List<String> a JSON String para campos JSON
        mensaje.setDiasActivos(convertirListaAJson(request.getDiasActivos()));
        mensaje.setGrupos(convertirListaAJson(request.getGrupos()));
        mensaje.setFlotas(convertirListaAJson(request.getFlotas()));
        
        // Guardar horasEspecificas como JSON (ya viene como JSON string del frontend)
        if (request.getHorasEspecificas() != null && !request.getHorasEspecificas().trim().isEmpty()) {
            // Validar que sea JSON válido
            try {
                // Verificar que sea JSON válido parseándolo
                objectMapper.readTree(request.getHorasEspecificas());
                mensaje.setHorasEspecificas(request.getHorasEspecificas());
                log.info("✅ [MarketingMensajeService] Horas específicas guardadas: {}", request.getHorasEspecificas());
            } catch (Exception e) {
                log.warn("⚠️ [MarketingMensajeService] Horas específicas no es JSON válido, guardando como está: {}", e.getMessage());
                mensaje.setHorasEspecificas(request.getHorasEspecificas());
            }
        }
        
        // Establecer valores por defecto
        if (request.getActivo() == null) {
            mensaje.setActivo(true);
        } else {
            mensaje.setActivo(request.getActivo());
        }

        MarketingMensaje mensajeGuardado = marketingMensajeRepository.save(mensaje);
        log.info("✅ [MarketingMensajeService] Mensaje creado exitosamente con ID: {}", mensajeGuardado.getId());

        // El envío de mensajes se realiza automáticamente por el scheduler según las horas programadas
        // No se envía inmediatamente al crear el mensaje
        if (request.getHorasEspecificas() != null && !request.getHorasEspecificas().trim().isEmpty()) {
            log.info("⏰ [MarketingMensajeService] Mensaje programado - se enviará según las horas específicas configuradas");
        } else if (request.getGrupos() != null && !request.getGrupos().isEmpty() && 
                   request.getWhatsapp() != null && request.getWhatsapp()) {
            log.info("ℹ️ [MarketingMensajeService] Mensaje sin horas específicas - no se enviará automáticamente");
        }

        return convertirAResponse(mensajeGuardado, "Mensaje creado exitosamente");
    }

    @Override
    @Transactional
    public MarketingMensajeResponse actualizarMensaje(Long id, MarketingMensajeRequest request, MultipartFile archivo) {
        log.info("🔄 [MarketingMensajeService] Actualizando mensaje con ID: {}", id);

        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("⚠️ [MarketingMensajeService] Mensaje con ID {} no encontrado", id);
                    return new IllegalArgumentException("Mensaje con ID " + id + " no encontrado");
                });

        // Guardar valor anterior necesario para comparación
        String tipoAnterior = mensaje.getTipo();

        // Manejar archivo nuevo si se proporciona
        if (archivo != null && !archivo.isEmpty()) {
            log.info("📤 [MarketingMensajeService] Subiendo nuevo archivo a MinIO: {}", archivo.getOriginalFilename());
            String nuevaUrlArchivo = minIOService.subirArchivo(archivo);
            if (nuevaUrlArchivo != null) {
                log.info("✅ [MarketingMensajeService] Archivo subido exitosamente: {}", nuevaUrlArchivo);
                mensaje.setArchivo(nuevaUrlArchivo);
            } else {
                log.warn("⚠️ [MarketingMensajeService] No se pudo subir el archivo a MinIO, manteniendo el archivo anterior");
            }
        } else if (request.getArchivo() != null) {
            // Si no hay archivo nuevo pero hay URL en el request, actualizar
            mensaje.setArchivo(request.getArchivo());
        }

        // Actualizar solo los campos proporcionados
        if (request.getTitulo() != null) {
            mensaje.setTitulo(request.getTitulo());
        }
        if (request.getMensaje() != null) {
            mensaje.setMensaje(request.getMensaje());
        }
        if (request.getModo() != null) {
            mensaje.setModo(request.getModo());
        }
        if (request.getTipo() != null) {
            String tipoNuevo = request.getTipo();
            
            // Si el tipo cambia a "ninguna", eliminar el archivo asociado
            if ("ninguna".equalsIgnoreCase(tipoNuevo.trim()) && 
                (tipoAnterior == null || !"ninguna".equalsIgnoreCase(tipoAnterior.trim()))) {
                String archivoActual = mensaje.getArchivo();
                if (archivoActual != null && !archivoActual.isEmpty()) {
                    log.info("🗑️ [MarketingMensajeService] Tipo cambió a 'ninguna', eliminando archivo: {}", archivoActual);
                    try {
                        // Intentar eliminar el archivo de MinIO
                        boolean eliminado = minIOService.eliminarArchivo(archivoActual);
                        if (eliminado) {
                            log.info("✅ [MarketingMensajeService] Archivo eliminado exitosamente de MinIO");
                        } else {
                            log.warn("⚠️ [MarketingMensajeService] No se pudo eliminar el archivo de MinIO, pero se limpiará la referencia");
                        }
                    } catch (Exception e) {
                        log.error("❌ [MarketingMensajeService] Error eliminando archivo de MinIO: {}", e.getMessage(), e);
                        // Continuar con la limpieza de la referencia aunque falle la eliminación física
                    }
                    // Limpiar la referencia al archivo en la base de datos
                    mensaje.setArchivo(null);
                    log.info("🧹 [MarketingMensajeService] Referencia al archivo eliminada del mensaje");
                }
            }
            
            mensaje.setTipo(tipoNuevo);
        }
        if (request.getWhatsapp() != null) {
            mensaje.setWhatsapp(request.getWhatsapp());
        }
        if (request.getYandex() != null) {
            mensaje.setYandex(request.getYandex());
        }
        if (request.getDiasActivos() != null) {
            List<String> diasLimpiados = limpiarListaDeJsonStrings(request.getDiasActivos());
            mensaje.setDiasActivos(convertirListaAJson(diasLimpiados));
            log.info("✅ [MarketingMensajeService] Días activos actualizados: {} -> {}", 
                request.getDiasActivos(), diasLimpiados);
        }
        if (request.getGrupos() != null) {
            List<String> gruposLimpiados = limpiarListaDeJsonStrings(request.getGrupos());
            mensaje.setGrupos(convertirListaAJson(gruposLimpiados));
            log.info("✅ [MarketingMensajeService] Grupos actualizados: {} -> {}", 
                request.getGrupos(), gruposLimpiados);
        }
        if (request.getFlotas() != null) {
            List<String> flotasLimpiadas = limpiarListaDeJsonStrings(request.getFlotas());
            mensaje.setFlotas(convertirListaAJson(flotasLimpiadas));
            log.info("✅ [MarketingMensajeService] Flotas actualizadas: {} -> {}", 
                request.getFlotas(), flotasLimpiadas);
        }
        if (request.getHorasEspecificas() != null && !request.getHorasEspecificas().trim().isEmpty()) {
            // Validar que sea JSON válido
            try {
                objectMapper.readTree(request.getHorasEspecificas());
                mensaje.setHorasEspecificas(request.getHorasEspecificas());
                log.info("✅ [MarketingMensajeService] Horas específicas actualizadas: {}", request.getHorasEspecificas());
            } catch (Exception e) {
                log.warn("⚠️ [MarketingMensajeService] Horas específicas no es JSON válido, guardando como está: {}", e.getMessage());
                mensaje.setHorasEspecificas(request.getHorasEspecificas());
            }
        }
        if (request.getActivo() != null) {
            mensaje.setActivo(request.getActivo());
        }

        MarketingMensaje mensajeActualizado = marketingMensajeRepository.save(mensaje);
        log.info("✅ [MarketingMensajeService] Mensaje actualizado exitosamente con ID: {}", mensajeActualizado.getId());

        // El envío de mensajes se realiza automáticamente por el scheduler según las horas programadas
        // No se envía inmediatamente al actualizar el mensaje, se respeta la hora programada
        if (mensajeActualizado.getHorasEspecificas() != null && !mensajeActualizado.getHorasEspecificas().trim().isEmpty()) {
            log.info("⏰ [MarketingMensajeService] Mensaje actualizado - se enviará según las horas específicas configuradas por el scheduler");
        } else {
            log.info("ℹ️ [MarketingMensajeService] Mensaje actualizado sin horas específicas - no se enviará automáticamente");
        }

        return convertirAResponse(mensajeActualizado, "Mensaje actualizado exitosamente");
    }

    @Override
    public MarketingMensajeResponse obtenerMensajePorId(Long id) {
        log.info("🔍 [MarketingMensajeService] Obteniendo mensaje con ID: {}", id);

        MarketingMensaje mensaje = marketingMensajeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("⚠️ [MarketingMensajeService] Mensaje con ID {} no encontrado", id);
                    return new IllegalArgumentException("Mensaje con ID " + id + " no encontrado");
                });

        return convertirAResponse(mensaje, null);
    }

    @Override
    public List<MarketingMensajeResponse> obtenerTodosLosMensajes() {
        log.info("📋 [MarketingMensajeService] Obteniendo todos los mensajes");
        List<MarketingMensaje> mensajes = marketingMensajeRepository.findAll();
        log.info("✅ [MarketingMensajeService] Se encontraron {} mensajes", mensajes.size());
        return mensajes.stream()
                .map(mensaje -> convertirAResponse(mensaje, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketingMensajeResponse> obtenerMensajesActivos() {
        log.info("📋 [MarketingMensajeService] Obteniendo mensajes activos");
        List<MarketingMensaje> mensajes = marketingMensajeRepository.findByActivoTrue();
        log.info("✅ [MarketingMensajeService] Se encontraron {} mensajes activos", mensajes.size());
        return mensajes.stream()
                .map(mensaje -> convertirAResponse(mensaje, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<MarketingMensajeResponse> obtenerMensajesPorTipo(String tipo) {
        log.info("📋 [MarketingMensajeService] Obteniendo mensajes por tipo: {}", tipo);
        List<MarketingMensaje> mensajes = marketingMensajeRepository.findByTipoAndActivoTrue(tipo);
        log.info("✅ [MarketingMensajeService] Se encontraron {} mensajes del tipo {}", mensajes.size(), tipo);
        return mensajes.stream()
                .map(mensaje -> convertirAResponse(mensaje, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void eliminarMensaje(Long id) {
        log.info("🗑️ [MarketingMensajeService] Eliminando mensaje con ID: {}", id);

        if (!marketingMensajeRepository.existsById(id)) {
            log.warn("⚠️ [MarketingMensajeService] Mensaje con ID {} no encontrado", id);
            throw new IllegalArgumentException("Mensaje con ID " + id + " no encontrado");
        }

        marketingMensajeRepository.deleteById(id);
        log.info("✅ [MarketingMensajeService] Mensaje eliminado exitosamente con ID: {}", id);
    }

    @Override
    public List<?> obtenerHistorico() {
        log.info("📋 [MarketingMensajeService] Obteniendo histórico");
        // TODO: Implementar lógica para obtener histórico
        throw new UnsupportedOperationException("Método obtenerHistorico() no implementado aún");
    }

    @Override
    public List<?> obtenerHistoricoPorMensajeId(Long mensajeId) {
        log.info("📋 [MarketingMensajeService] Obteniendo histórico para mensaje ID: {}", mensajeId);
        // TODO: Implementar lógica para obtener histórico por mensaje
        throw new UnsupportedOperationException("Método obtenerHistoricoPorMensajeId() no implementado aún");
    }

    @Override
    public List<GrupoWhatsAppResponse> obtenerGrupos() {
        long now = System.currentTimeMillis();
        if (gruposCache != null && now < gruposCacheExpiry) {
            log.debug("📋 [MarketingMensajeService] Grupos desde caché ({} entradas)", gruposCache.size());
            return gruposCache;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (gruposCache != null && now < gruposCacheExpiry) {
                return gruposCache;
            }
            log.info("📋 [MarketingMensajeService] Obteniendo grupos de WhatsApp desde la base de datos");
            try {
                List<ModuleMarketingGroup> gruposBD = moduleMarketingGroupRepository.findAll();
                List<GrupoWhatsAppResponse> grupos = gruposBD.stream()
                        .map(grupo -> {
                            GrupoWhatsAppResponse response = new GrupoWhatsAppResponse();
                            response.setId(grupo.getGroupId());
                            response.setSubject(grupo.getSubject());
                            response.setPictureUrl(grupo.getPictureUrl());
                            return response;
                        })
                        .collect(Collectors.toList());
                gruposCache = grupos;
                gruposCacheExpiry = now + CACHE_TTL_MS;
                log.info("✅ [MarketingMensajeService] Se obtuvieron {} grupos de WhatsApp desde la BD (caché 5 min)", grupos.size());
                return grupos;
            } catch (Exception e) {
                log.error("❌ [MarketingMensajeService] Error obteniendo grupos de WhatsApp desde la BD: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FlotaResponse> obtenerFlotas() {
        long now = System.currentTimeMillis();
        if (flotasCache != null && now < flotasCacheExpiry) {
            log.debug("📋 [MarketingMensajeService] Flotas desde caché ({} entradas)", flotasCache.size());
            return flotasCache;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (flotasCache != null && now < flotasCacheExpiry) {
                return flotasCache;
            }
            log.info("📋 [MarketingMensajeService] Obteniendo TODAS las flotas desde API externa");
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
                log.info("✅ [MarketingMensajeService] Se obtuvieron {} flotas (caché 5 min)", flotas.size());
                return flotas;
            } catch (Exception e) {
                log.error("❌ [MarketingMensajeService] Error obteniendo flotas: {}", e.getMessage(), e);
                return Collections.emptyList();
            }
        }
    }

    @Override
    public List<MarketingMensajeCalendarioResponse> obtenerMensajesParaCalendario() {
        log.info("📅 [MarketingMensajeService] Obteniendo mensajes para calendario");
        List<MarketingMensaje> mensajes = marketingMensajeRepository.findAll();
        log.info("✅ [MarketingMensajeService] Se encontraron {} mensajes para calendario", mensajes.size());
        return mensajes.stream()
                .map(this::convertirACalendarioResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad MarketingMensaje a MarketingMensajeCalendarioResponse
     * Solo incluye los campos necesarios para el calendario
     */
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
        
        // Convertir JSON String a List<String> para campos JSON
        // Limpiar antes de convertir para manejar datos mal serializados
        response.setDiasActivos(normalizarDiasActivos(convertirJsonALista(entity.getDiasActivos())));
        response.setGrupos(convertirJsonALista(entity.getGrupos()));
        response.setFlotas(convertirJsonALista(entity.getFlotas()));
        
        // Horas específicas se devuelve como JSON string
        response.setHorasEspecificas(entity.getHorasEspecificas());
        
        return response;
    }

    /**
     * Normaliza los días activos al formato abreviado en español para el frontend
     * @param dias Lista de días en cualquier formato
     * @return Lista de días normalizados al formato abreviado (Lun, Mar, Mié, Jue, Vie, Sáb, Dom)
     */
    private List<String> normalizarDiasActivos(List<String> dias) {
        if (dias == null || dias.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, String> mapaDias = new java.util.HashMap<>();
        mapaDias.put("lunes", "Lun");
        mapaDias.put("martes", "Mar");
        mapaDias.put("miercoles", "Mié");
        mapaDias.put("miércoles", "Mié");
        mapaDias.put("jueves", "Jue");
        mapaDias.put("viernes", "Vie");
        mapaDias.put("sabado", "Sáb");
        mapaDias.put("sábado", "Sáb");
        mapaDias.put("domingo", "Dom");
        mapaDias.put("Lun", "Lun");
        mapaDias.put("Mar", "Mar");
        mapaDias.put("Mié", "Mié");
        mapaDias.put("Jue", "Jue");
        mapaDias.put("Vie", "Vie");
        mapaDias.put("Sáb", "Sáb");
        mapaDias.put("Dom", "Dom");
        mapaDias.put("Monday", "Lun");
        mapaDias.put("Tuesday", "Mar");
        mapaDias.put("Wednesday", "Mié");
        mapaDias.put("Thursday", "Jue");
        mapaDias.put("Friday", "Vie");
        mapaDias.put("Saturday", "Sáb");
        mapaDias.put("Sunday", "Dom");
        
        return dias.stream()
                .map(dia -> {
                    String diaLower = dia != null ? dia.toLowerCase().trim() : "";
                    String diaNormalizado = mapaDias.get(diaLower);
                    if (diaNormalizado == null) {
                        // Si no se encuentra en el mapa, intentar buscar con primera letra mayúscula
                        if (dia != null && !dia.isEmpty()) {
                            String diaWithCapital = dia.substring(0, 1).toUpperCase() + (dia.length() > 1 ? dia.substring(1).toLowerCase() : "");
                            diaNormalizado = mapaDias.get(diaWithCapital.toLowerCase());
                        }
                    }
                    return diaNormalizado != null ? diaNormalizado : dia; // Si no se encuentra, devolver el original
                })
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el ID del usuario autenticado desde el contexto de seguridad
    */
    private Long obtenerUserIdActual() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
                String userIdString = authentication.getName();
                Long userId = Long.parseLong(userIdString);
                log.debug("👤 [MarketingMensajeService] Usuario autenticado ID: {}", userId);
                return userId;
            } else {
                log.warn("⚠️ [MarketingMensajeService] No se pudo obtener usuario autenticado del contexto de seguridad");
                return null;
            }
        } catch (NumberFormatException e) {
            log.error("❌ [MarketingMensajeService] Error parseando userId del contexto de seguridad: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error obteniendo usuario del contexto de seguridad: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convierte una List<String> a JSON String
     */
    private String convertirListaAJson(List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error convirtiendo lista a JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convierte un JSON String a List<String>
     * Maneja casos de doble/triple serialización limpiando los datos antes de parsear
     */
    private List<String> convertirJsonALista(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String jsonLimpio = limpiarJsonString(json);
        
        try {
            List<String> resultado = objectMapper.readValue(jsonLimpio, new TypeReference<List<String>>() {});
            
            // Si el resultado contiene JSON strings, limpiarlos recursivamente
            List<String> resultadoLimpio = new ArrayList<>();
            for (String item : resultado) {
                if (item != null && item.trim().startsWith("[") && item.trim().endsWith("]")) {
                    // Es otro JSON string, parsearlo recursivamente
                    List<String> subLista = convertirJsonALista(item);
                    resultadoLimpio.addAll(subLista);
                } else {
                    resultadoLimpio.add(item);
                }
            }
            
            return resultadoLimpio;
        } catch (Exception e) {
            log.warn("⚠️ [MarketingMensajeService] Error convirtiendo JSON a lista (intentando limpiar): {} | JSON: {}", 
                e.getMessage(), json.substring(0, Math.min(100, json.length())));
            return Collections.emptyList();
        }
    }
    
    /**
     * Limpia un JSON string que puede estar múltiples veces serializado
     * Intenta parsear recursivamente hasta obtener el JSON base
     */
    private String limpiarJsonString(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }
        
        String jsonLimpio = json.trim();
        int intentos = 0;
        final int MAX_INTENTOS = 5; // Evitar loops infinitos
        
        while (intentos < MAX_INTENTOS) {
            try {
                // Intentar parsear
                Object parsed = objectMapper.readValue(jsonLimpio, Object.class);
                
                // Si es una lista con un solo elemento que es un string que parece JSON
                if (parsed instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> lista = (List<Object>) parsed;
                    
                    if (lista.size() == 1 && lista.get(0) instanceof String) {
                        String elemento = (String) lista.get(0);
                        if ((elemento.trim().startsWith("[") && elemento.trim().endsWith("]")) ||
                            (elemento.trim().startsWith("{") && elemento.trim().endsWith("}"))) {
                            // Es otro JSON string anidado, continuar limpiando
                            jsonLimpio = elemento;
                            intentos++;
                            continue;
                        }
                    }
                }
                
                // Si llegamos aquí, el JSON está limpio
                break;
            } catch (Exception e) {
                // No se puede parsear más, retornar el JSON actual
                break;
            }
        }
        
        return jsonLimpio;
    }
    
    /**
     * Limpia una lista que puede contener JSON strings anidados
     */
    private List<String> limpiarListaDeJsonStrings(List<String> lista) {
        if (lista == null || lista.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> resultado = new ArrayList<>();
        
        for (String item : lista) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            
            String itemTrimmed = item.trim();
            
            // Si es un JSON string, parsearlo
            if ((itemTrimmed.startsWith("[") && itemTrimmed.endsWith("]")) ||
                (itemTrimmed.startsWith("{") && itemTrimmed.endsWith("}"))) {
                try {
                    Object parsed = objectMapper.readValue(itemTrimmed, Object.class);
                    
                    if (parsed instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> parsedList = (List<Object>) parsed;
                        
                        if (parsedList.isEmpty()) {
                            // Array vacío, ignorar
                            continue;
                        }
                        
                        for (Object element : parsedList) {
                            if (element != null) {
                                String elementoStr = element.toString();
                                // Si el elemento es otro JSON string, parsearlo recursivamente
                                if (elementoStr.trim().startsWith("[") && elementoStr.trim().endsWith("]")) {
                                    List<String> subLista = limpiarListaDeJsonStrings(Collections.singletonList(elementoStr));
                                    resultado.addAll(subLista);
                                } else {
                                    resultado.add(elementoStr);
                                }
                            }
                        }
                    } else {
                        resultado.add(item);
                    }
                } catch (Exception e) {
                    log.debug("⚠️ [MarketingMensajeService] No se pudo parsear como JSON, usando valor original: {}", item);
                    resultado.add(item);
                }
            } else {
                resultado.add(item);
            }
        }
        
        return resultado;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Aplica filtros y orden por fecha (más recientes primero) para exportación. */
    private List<MarketingMensajeResponse> filtrarYOrdenarParaExport(
            String searchTerm, String modo, String tipo, String canales) {
        List<MarketingMensajeResponse> list = obtenerTodosLosMensajes();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String q = searchTerm.trim().toLowerCase();
            list = list.stream().filter(m ->
                (m.getTitulo() != null && m.getTitulo().toLowerCase().contains(q)) ||
                (m.getMensaje() != null && m.getMensaje().toLowerCase().contains(q)) ||
                (m.getModo() != null && m.getModo().toLowerCase().contains(q)) ||
                (m.getTipo() != null && m.getTipo().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        if (modo != null && !modo.isEmpty() && !"all".equalsIgnoreCase(modo)) {
            list = list.stream().filter(m -> modo.equals(m.getModo())).collect(Collectors.toList());
        }
        if (tipo != null && !tipo.isEmpty() && !"all".equalsIgnoreCase(tipo)) {
            list = list.stream().filter(m -> tipo.equals(m.getTipo())).collect(Collectors.toList());
        }
        if (canales != null && !canales.isEmpty() && !"all".equalsIgnoreCase(canales)) {
            if ("whatsapp".equalsIgnoreCase(canales)) {
                list = list.stream().filter(m -> Boolean.TRUE.equals(m.getWhatsapp())).collect(Collectors.toList());
            } else if ("yandex".equalsIgnoreCase(canales)) {
                list = list.stream().filter(m -> Boolean.TRUE.equals(m.getYandex())).collect(Collectors.toList());
            } else if ("ambos".equalsIgnoreCase(canales)) {
                list = list.stream().filter(m -> Boolean.TRUE.equals(m.getWhatsapp()) && Boolean.TRUE.equals(m.getYandex())).collect(Collectors.toList());
            }
        }
        // Orden: más recientes primero
        list = list.stream().sorted((a, b) -> {
            long ta = a.getCreatedAt() != null ? a.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;
            long tb = b.getCreatedAt() != null ? b.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;
            return Long.compare(tb, ta);
        }).collect(Collectors.toList());
        return list;
    }

    @Override
    public byte[] exportarTodosMensajesExcel(String searchTerm, String modo, String tipo, String canales) {
        List<MarketingMensajeResponse> mensajes = filtrarYOrdenarParaExport(searchTerm, modo, tipo, canales);
        log.info("📊 [MarketingMensajeService] Exportando {} mensajes a Excel (filtros aplicados)", mensajes.size());
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
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = { "ID", "Título", "Mensaje", "Modo", "Tipo", "WhatsApp", "Yandex", "Días activos", "Grupos", "Flotas", "Horas específicas", "Activo", "Creado", "Actualizado" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            for (MarketingMensajeResponse m : mensajes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getId() != null ? m.getId() : 0);
                setCell(row, 1, m.getTitulo(), dataStyle);
                setCell(row, 2, m.getMensaje(), dataStyle);
                setCell(row, 3, m.getModo(), dataStyle);
                setCell(row, 4, m.getTipo(), dataStyle);
                setCell(row, 5, m.getWhatsapp() != null && m.getWhatsapp() ? "Sí" : "No", dataStyle);
                setCell(row, 6, m.getYandex() != null && m.getYandex() ? "Sí" : "No", dataStyle);
                setCell(row, 7, m.getDiasActivos() != null ? String.join(", ", m.getDiasActivos()) : "", dataStyle);
                String gruposNombres = m.getGrupos() != null ? m.getGrupos().stream()
                        .map(id -> grupoIdToName.getOrDefault(id, id))
                        .collect(Collectors.joining(", ")) : "";
                String flotasNombres = m.getFlotas() != null ? m.getFlotas().stream()
                        .map(id -> flotaIdToName.getOrDefault(id, id))
                        .collect(Collectors.joining(", ")) : "";
                setCell(row, 8, gruposNombres, dataStyle);
                setCell(row, 9, flotasNombres, dataStyle);
                setCell(row, 10, m.getHorasEspecificas() != null ? m.getHorasEspecificas() : "", dataStyle);
                setCell(row, 11, m.getActivo() != null && m.getActivo() ? "Sí" : "No", dataStyle);
                setCell(row, 12, m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FORMATTER) : "", dataStyle);
                setCell(row, 13, m.getUpdatedAt() != null ? m.getUpdatedAt().format(DATE_FORMATTER) : "", dataStyle);
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("✅ [MarketingMensajeService] Excel generado con {} mensajes", mensajes.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error generando Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando Excel de mensajes", e);
        }
    }

    private static void setCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    @Override
    public byte[] exportarTodosMensajesPdf(String searchTerm, String modo, String tipo, String canales) {
        List<MarketingMensajeResponse> mensajes = filtrarYOrdenarParaExport(searchTerm, modo, tipo, canales);
        log.info("📄 [MarketingMensajeService] Exportando {} mensajes a PDF (filtros aplicados)", mensajes.size());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Listado de mensajes - SMS Marketing YEGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{1f, 2f, 3f, 1f, 1f, 2f, 2f, 2f});
            table.addCell(crearCeldaHeader("ID"));
            table.addCell(crearCeldaHeader("Título"));
            table.addCell(crearCeldaHeader("Mensaje"));
            table.addCell(crearCeldaHeader("Modo"));
            table.addCell(crearCeldaHeader("Tipo"));
            table.addCell(crearCeldaHeader("Días"));
            table.addCell(crearCeldaHeader("Canales"));
            table.addCell(crearCeldaHeader("Creado"));
            for (MarketingMensajeResponse m : mensajes) {
                table.addCell(crearCelda(m.getId() != null ? m.getId().toString() : ""));
                table.addCell(crearCelda(m.getTitulo()));
                table.addCell(crearCelda(truncar(m.getMensaje(), 80)));
                table.addCell(crearCelda(m.getModo()));
                table.addCell(crearCelda(m.getTipo()));
                table.addCell(crearCelda(m.getDiasActivos() != null ? String.join(", ", m.getDiasActivos()) : ""));
                String textoCanales = "";
                if (Boolean.TRUE.equals(m.getWhatsapp())) textoCanales = "WhatsApp";
                if (Boolean.TRUE.equals(m.getYandex())) textoCanales += (textoCanales.isEmpty() ? "" : ", ") + "Yandex";
                table.addCell(crearCelda(textoCanales));
                table.addCell(crearCelda(m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FORMATTER) : ""));
            }
            document.add(table);
            document.close();
            log.info("✅ [MarketingMensajeService] PDF generado con {} mensajes", mensajes.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando PDF de mensajes", e);
        }
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

