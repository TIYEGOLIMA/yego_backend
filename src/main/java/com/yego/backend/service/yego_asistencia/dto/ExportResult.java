package com.yego.backend.service.yego_asistencia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resultado de exportación de marcaciones (Excel).
 * La capa de servicio no decide códigos HTTP; el controlador mapea hasContent() a 200/404.
 */
@Getter
@AllArgsConstructor
public class ExportResult {
    private final byte[] content;
    private final String fileName;

    public boolean hasContent() {
        return content != null && content.length > 0;
    }
}
