package com.yego.backend.service.yego_marketing_mensajes.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.MinIOService;
import com.yego.backend.service.WhatsAppService;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MinIOService minIOService;
    private final WhatsAppService whatsAppService;

    public MarketingMensajeServiceImpl(MarketingMensajeRepository marketingMensajeRepository, RestTemplate restTemplate, MinIOService minIOService, WhatsAppService whatsAppService) {
        this.marketingMensajeRepository = marketingMensajeRepository;
        this.restTemplate = restTemplate;
        this.minIOService = minIOService;
        this.whatsAppService = whatsAppService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public MarketingMensajeResponse crearMensaje(MarketingMensajeRequest request, MultipartFile archivo) {
        log.info("📝 [MarketingMensajeService] Creando nuevo mensaje: {}", request.getTitulo());

        // Validar que no exista un mensaje con el mismo título
        if (marketingMensajeRepository.existsByTitulo(request.getTitulo())) {
            log.warn("⚠️ [MarketingMensajeService] Ya existe un mensaje con el título: {}", request.getTitulo());
            throw new IllegalArgumentException("Ya existe un mensaje con el título: " + request.getTitulo());
        }

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

        // Validar que el nuevo título no esté en uso por otro mensaje
        if (request.getTitulo() != null && !request.getTitulo().equals(mensaje.getTitulo())) {
            if (marketingMensajeRepository.existsByTitulo(request.getTitulo())) {
                log.warn("⚠️ [MarketingMensajeService] Ya existe un mensaje con el título: {}", request.getTitulo());
                throw new IllegalArgumentException("Ya existe un mensaje con el título: " + request.getTitulo());
            }
        }

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
            mensaje.setDiasActivos(convertirListaAJson(request.getDiasActivos()));
        }
        if (request.getGrupos() != null) {
            mensaje.setGrupos(convertirListaAJson(request.getGrupos()));
        }
        if (request.getFlotas() != null) {
            mensaje.setFlotas(convertirListaAJson(request.getFlotas()));
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
        log.info("📋 [MarketingMensajeService] Obteniendo grupos de WhatsApp");
        
        try {
            String url = "https://wsp.yego.pro/group/fetchAllGroups/TEAM_PERU?getParticipants=false";
            String apiKey = "f81bd660c7c2a537b63fc1ecda476ae6";
            
            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Apikey", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Realizar petición GET
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            List<GrupoWhatsAppResponse> grupos = new ArrayList<>();
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> gruposData = response.getBody();
                
                for (Map<String, Object> grupoData : gruposData) {
                    GrupoWhatsAppResponse grupo = new GrupoWhatsAppResponse();
                    grupo.setId(grupoData.get("id") != null ? grupoData.get("id").toString() : null);
                    grupo.setSubject(grupoData.get("subject") != null ? grupoData.get("subject").toString() : null);
                    grupos.add(grupo);
                    log.debug("✅ [MarketingMensajeService] Grupo agregado: {} - {}", grupo.getId(), grupo.getSubject());
                }
            }
            
            log.info("✅ [MarketingMensajeService] Se obtuvieron {} grupos de WhatsApp", grupos.size());
            return grupos;
            
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error obteniendo grupos de WhatsApp: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FlotaResponse> obtenerFlotas() {
        log.info("📋 [MarketingMensajeService] Obteniendo TODAS las flotas desde API externa");
        
        try {
            String url = "http://162.55.214.109:6000/v2/partners";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            
            List<FlotaResponse> flotas = new ArrayList<>();
            
            if (response != null && response.containsKey("partners")) {
                List<Map<String, Object>> partners = (List<Map<String, Object>>) response.get("partners");
                
                // Convertir TODAS las flotas sin filtrar
                for (Map<String, Object> item : partners) {
                    FlotaResponse flota = new FlotaResponse();
                    flota.setId(item.get("id").toString());
                    flota.setName(item.get("name").toString());
                    flota.setCity(item.get("city") != null ? item.get("city").toString() : null);
                    flota.setSpecifications((List<String>) item.get("specifications"));
                    flotas.add(flota);
                    log.debug("✅ [MarketingMensajeService] Flota agregada: {} - {}", flota.getId(), flota.getName());
                }
            }
            
            log.info("✅ [MarketingMensajeService] Se obtuvieron {} flotas (TODAS sin filtrar)", flotas.size());
            return flotas;
            
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error obteniendo flotas: {}", e.getMessage(), e);
            return Collections.emptyList();
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
     */
    private List<String> convertirJsonALista(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("❌ [MarketingMensajeService] Error convirtiendo JSON a lista: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

