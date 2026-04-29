package com.yego.backend.entity.yego_gantt.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "area_task_attachments",
        indexes = @Index(name = "idx_area_task_attachments_task", columnList = "task_id")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaTaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * Clave u objeto en almacén; si el gateway de MinIO devuelve URL pública, se persiste aquí para borrado.
     */
    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
