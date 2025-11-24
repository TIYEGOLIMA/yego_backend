package com.yego.backend.service.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Servicio para operaciones relacionadas con mensajes de marketing
 * Maneja la creación, actualización y gestión de mensajes
 * 
 * @author Sistema Yego
 * @version 1.0
 */
public interface MarketingMensajeService {
    
    /**
     * Crea un nuevo mensaje de marketing
     * @param request datos del mensaje a crear
     * @param archivo archivo opcional a subir a MinIO
     * @return respuesta con el mensaje creado
     */
    MarketingMensajeResponse crearMensaje(MarketingMensajeRequest request, MultipartFile archivo);
    
    /**
     * Actualiza un mensaje de marketing existente
     * @param id ID del mensaje a actualizar
     * @param request datos actualizados del mensaje
     * @param archivo archivo opcional a subir a MinIO
     * @return respuesta con el mensaje actualizado
     */
    MarketingMensajeResponse actualizarMensaje(Long id, MarketingMensajeRequest request, MultipartFile archivo);
    
    /**
     * Obtiene un mensaje por su ID
     * @param id ID del mensaje
     * @return respuesta con el mensaje encontrado
     */
    MarketingMensajeResponse obtenerMensajePorId(Long id);
    
    /**
     * Obtiene todos los mensajes
     * @return lista de mensajes
     */
    List<MarketingMensajeResponse> obtenerTodosLosMensajes();
    
    /**
     * Obtiene solo los mensajes activos
     * @return lista de mensajes activos
     */
    List<MarketingMensajeResponse> obtenerMensajesActivos();
    
    /**
     * Obtiene mensajes por tipo
     * @param tipo Tipo de mensaje
     * @return lista de mensajes del tipo especificado
     */
    List<MarketingMensajeResponse> obtenerMensajesPorTipo(String tipo);
    
    /**
     * Elimina un mensaje (eliminación lógica o física)
     * @param id ID del mensaje a eliminar
     */
    void eliminarMensaje(Long id);
    
    /**
     * Obtiene el histórico de todas las acciones
     * @return lista del histórico
     */
    List<?> obtenerHistorico();
    
    /**
     * Obtiene el histórico de un mensaje específico
     * @param mensajeId ID del mensaje
     * @return lista del histórico del mensaje
     */
    List<?> obtenerHistoricoPorMensajeId(Long mensajeId);
    
    /**
     * Obtiene los grupos disponibles de WhatsApp desde la base de datos
     * @return lista de grupos con id, subject y pictureUrl
     */
    List<GrupoWhatsAppResponse> obtenerGrupos();
    
    /**
     * Obtiene las flotas disponibles
     * @return lista de flotas
     */
    List<FlotaResponse> obtenerFlotas();
    
    /**
     * Obtiene todos los mensajes para el calendario
     * Solo incluye los campos necesarios para el calendario: id, titulo, diasActivos, horaInicio, horaFin
     * @return lista de mensajes para el calendario
     */
    List<MarketingMensajeCalendarioResponse> obtenerMensajesParaCalendario();
}

