package com.yego.backend.service.yego_ticketerera;
import org.springframework.http.ResponseEntity;

public interface SacStatsExportService {
    
    ResponseEntity<byte[]> exportarAExcel();
    
    ResponseEntity<byte[]> exportarAImagen(String formato);
    
    String obtenerNombreArchivoExcel();
    
    String obtenerNombreArchivoImagen(String formato);
}
