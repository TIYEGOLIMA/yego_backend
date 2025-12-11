package com.yego.backend.service.yego_ticketerera;
import org.springframework.http.ResponseEntity;

public interface SacStatsExportService {
    
    ResponseEntity<byte[]> exportarAExcel(String fechaInicio, String fechaFin);
    
    ResponseEntity<byte[]> exportarAImagen(String formato, String fechaInicio, String fechaFin);
    
    String obtenerNombreArchivoExcel(String fechaInicio, String fechaFin);
    
    String obtenerNombreArchivoImagen(String formato, String fechaInicio, String fechaFin);
}
