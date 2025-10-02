package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.api.response.SacStatsResponse;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;

public interface SacStatsExportService {
    
    ResponseEntity<byte[]> exportarAExcel();
    
    ResponseEntity<byte[]> exportarAImagen(String formato);
    
    String obtenerNombreArchivoExcel();
    
    String obtenerNombreArchivoImagen(String formato);
}
