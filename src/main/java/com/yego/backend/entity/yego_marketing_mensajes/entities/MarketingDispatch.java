package com.yego.backend.entity.yego_marketing_mensajes.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "module_marketing_dispatches",
        indexes = {
                @Index(
                        name = "idx_marketing_dispatch_status_updated",
                        columnList = "status, updated_at"),
                @Index(
                        name = "idx_marketing_dispatch_message_scheduled",
                        columnList = "message_id, scheduled_for")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uq_marketing_dispatch",
                columnNames = {"message_id", "channel", "destination_id", "scheduled_for"}))
@Getter
@Setter
@NoArgsConstructor
public class MarketingDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "destination_id", nullable = false, columnDefinition = "TEXT")
    private String destinationId;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "provider_message_id", columnDefinition = "TEXT")
    private String providerMessageId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
