package com.yego.backend.repository.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketingDispatchRepository extends JpaRepository<MarketingDispatch, Long> {

    Optional<MarketingDispatch> findByMessageIdAndChannelAndDestinationIdAndScheduledFor(
            Long messageId, String channel, String destinationId, Instant scheduledFor);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO module_marketing_dispatches (
                message_id, channel, destination_id, scheduled_for, status,
                attempt_count, idempotency_key, created_at, updated_at
            ) VALUES (
                :messageId, :channel, :destinationId, :scheduledFor, 'PROCESSING',
                1, :idempotencyKey, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (message_id, channel, destination_id, scheduled_for)
            DO UPDATE SET
                status = 'PROCESSING',
                attempt_count = module_marketing_dispatches.attempt_count + 1,
                http_status = NULL,
                error_message = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE (
                module_marketing_dispatches.status = 'FAILED'
                AND module_marketing_dispatches.attempt_count < :maxAttempts
                AND module_marketing_dispatches.updated_at
                    <= CURRENT_TIMESTAMP - make_interval(secs => :retryDelaySeconds)
            ) OR (
                module_marketing_dispatches.status = 'PROCESSING'
                AND module_marketing_dispatches.updated_at
                    <= CURRENT_TIMESTAMP - make_interval(secs => :processingTimeoutSeconds)
            )
            """, nativeQuery = true)
    int claim(
            @Param("messageId") Long messageId,
            @Param("channel") String channel,
            @Param("destinationId") String destinationId,
            @Param("scheduledFor") Instant scheduledFor,
            @Param("idempotencyKey") UUID idempotencyKey,
            @Param("maxAttempts") int maxAttempts,
            @Param("retryDelaySeconds") int retryDelaySeconds,
            @Param("processingTimeoutSeconds") int processingTimeoutSeconds);

    @Modifying
    @Transactional
    @Query("""
            UPDATE MarketingDispatch dispatch
            SET dispatch.status = :status,
                dispatch.httpStatus = :httpStatus,
                dispatch.providerMessageId = :providerMessageId,
                dispatch.errorMessage = :errorMessage,
                dispatch.sentAt = :sentAt,
                dispatch.updatedAt = :updatedAt
            WHERE dispatch.id = :id
              AND dispatch.status = 'PROCESSING'
            """)
    int complete(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("httpStatus") Integer httpStatus,
            @Param("providerMessageId") String providerMessageId,
            @Param("errorMessage") String errorMessage,
            @Param("sentAt") Instant sentAt,
            @Param("updatedAt") Instant updatedAt);
}
