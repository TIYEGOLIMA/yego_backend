package com.yego.backend.service.yego_ticketerera;
import org.springframework.http.ResponseEntity;

public interface SacStatsExportService {
    
    ResponseEntity<byte[]> exportarAExcel(String fechaInicio, String fechaFin, Long sedeId);
    
    ResponseEntity<byte[]> exportarAImagen(String formato, String fechaInicio, String fechaFin, Long sedeId);
}
