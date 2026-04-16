package com.yego.backend.service.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeCalendarioResponse;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.GrupoWhatsAppResponse;
import com.yego.backend.entity.yego_garantizado.api.response.FlotaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MarketingMensajeService {
    
    MarketingMensajeResponse crearMensaje(MarketingMensajeRequest request, MultipartFile archivo);
    
    MarketingMensajeResponse actualizarMensaje(Long id, MarketingMensajeRequest request, MultipartFile archivo);
    
    MarketingMensajeResponse obtenerMensajePorId(Long id);
    
    List<MarketingMensajeResponse> obtenerTodosLosMensajes();
    
    List<MarketingMensajeResponse> obtenerMensajesActivos();
    
    List<MarketingMensajeResponse> obtenerMensajesPorTipo(String tipo);
    
    void eliminarMensaje(Long id);
    
    List<GrupoWhatsAppResponse> obtenerGrupos();
    
    List<FlotaResponse> obtenerFlotas();
    
    List<MarketingMensajeCalendarioResponse> obtenerMensajesParaCalendario();

    byte[] exportarTodosMensajesExcel(String searchTerm, String modo, String tipo, String canales, String fechaDesde, String fechaHasta);

    byte[] exportarTodosMensajesPdf(String searchTerm, String modo, String tipo, String canales, String fechaDesde, String fechaHasta);

    void invalidarCacheGrupos();
}
