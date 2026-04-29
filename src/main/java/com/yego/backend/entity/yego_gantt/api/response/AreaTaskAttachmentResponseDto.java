package com.yego.backend.entity.yego_gantt.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTaskAttachmentResponseDto {
    private Long id;
    private Long taskId;
    /** URL o clave devuelta por el almacén (descarga vía gateway). */
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
    private Long createdByUserId;
}
