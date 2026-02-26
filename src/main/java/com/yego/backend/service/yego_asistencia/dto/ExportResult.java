package com.yego.backend.service.yego_asistencia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de exportación de marcaciones (Excel).
 * La capa de servicio no decide códigos HTTP; el controlador mapea hasContent() a 200/404.
 * Si no hay contenido, message puede indicar el motivo (ej. no hay datos para el área).
 */
@Getter
@AllArgsConstructor
public class ExportResult {
    private final byte[] content;
    private final String fileName;
    /** Mensaje cuando no hay contenido (para 404). */
    private final String message;

    public ExportResult(byte[] content, String fileName) {
        this.content = content;
        this.fileName = fileName;
        this.message = null;
    }

    public boolean hasContent() {
        return content != null && content.length > 0;
    }
}
