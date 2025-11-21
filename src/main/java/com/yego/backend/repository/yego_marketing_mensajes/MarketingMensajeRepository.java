package com.yego.backend.repository.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para operaciones con la tabla yego_marketing_mensajes
 * Maneja la persistencia de mensajes de marketing
 * 
 * @author Sistema Yego
 * @version 1.0
 */
@Repository
public interface MarketingMensajeRepository extends JpaRepository<MarketingMensaje, Long> {
    
    /**
     * Busca todos los mensajes activos
     * @return Lista de mensajes activos
     */
    List<MarketingMensaje> findByActivoTrue();
    
    /**
     * Busca mensajes por tipo
     * @param tipo Tipo de mensaje a buscar
     * @return Lista de mensajes del tipo especificado
     */
    List<MarketingMensaje> findByTipo(String tipo);
    
    /**
     * Busca mensajes activos por tipo
     * @param tipo Tipo de mensaje a buscar
     * @return Lista de mensajes activos del tipo especificado
     */
    List<MarketingMensaje> findByTipoAndActivoTrue(String tipo);
    
    /**
     * Verifica si existe un mensaje con el título dado
     * @param titulo Título a verificar
     * @return true si existe, false si no
     */
    boolean existsByTitulo(String titulo);
    
    /**
     * Busca mensajes activos con WhatsApp activado y horas específicas configuradas
     * @return Lista de mensajes que pueden ser enviados programadamente
     */
    List<MarketingMensaje> findByActivoTrueAndWhatsappTrueAndHorasEspecificasIsNotNull();
}

