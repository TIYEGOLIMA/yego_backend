package com.yego.backend.repository.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketingMensajeRepository extends JpaRepository<MarketingMensaje, Long> {
    
    List<MarketingMensaje> findByActivoTrue();
    
    List<MarketingMensaje> findByTipoAndActivoTrue(String tipo);
    
    List<MarketingMensaje> findByActivoTrueAndHorasEspecificasIsNotNull();
}
