package com.yego.backend.service.yego_marketing_mensajes.sender;

import com.yego.backend.repository.yego_marketing_mensajes.MarketingDispatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class MarketingDispatchTracker {

    public static final String CHANNEL_WHATSAPP = "WHATSAPP";
    public static final String CHANNEL_FLEET = "FLEET";

    private final MarketingDispatchRepository repository;
    private final int maxAttempts;
    private final int retryDelaySeconds;
    private final int processingTimeoutSeconds;

    public MarketingDispatchTracker(
            MarketingDispatchRepository repository,
            @Value("${marketing.delivery.max-attempts:3}") int maxAttempts,
            @Value("${marketing.delivery.retry-delay-seconds:60}") int retryDelaySeconds,
            @Value("${marketing.delivery.processing-timeout-seconds:180}") int processingTimeoutSeconds) {
        this.repository = repository;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelaySeconds = Math.max(0, retryDelaySeconds);
        this.processingTimeoutSeconds = Math.max(30, processingTimeoutSeconds);
    }

    public Optional<Claim> reclamar(
            Long messageId, String channel, String destinationId, Instant scheduledFor) {
        int claimed = repository.claim(
                messageId,
                channel,
                destinationId,
                scheduledFor,
                UUID.randomUUID(),
                maxAttempts,
                retryDelaySeconds,
                processingTimeoutSeconds);
        if (claimed == 0) {
            return Optional.empty();
        }
        return repository.findByMessageIdAndChannelAndDestinationIdAndScheduledFor(
                        messageId, channel, destinationId, scheduledFor)
                .map(dispatch -> new Claim(
                        dispatch.getId(),
                        dispatch.getIdempotencyKey(),
                        dispatch.getAttemptCount()));
    }

    public void marcarEnviado(Claim claim, Integer httpStatus, String providerMessageId) {
        Instant now = Instant.now();
        verificarActualizacion(
                repository.complete(
                        claim.id(), "SENT", httpStatus, providerMessageId, null, now, now),
                claim,
                "SENT");
    }

    public void marcarFallido(Claim claim, Integer httpStatus, String errorMessage) {
        verificarActualizacion(
                repository.complete(
                        claim.id(),
                        "FAILED",
                        httpStatus,
                        null,
                        truncate(errorMessage),
                        null,
                        Instant.now()),
                claim,
                "FAILED");
    }

    public void marcarOmitido(Claim claim, Integer httpStatus, String reason) {
        verificarActualizacion(
                repository.complete(
                        claim.id(),
                        "SKIPPED",
                        httpStatus,
                        null,
                        truncate(reason),
                        null,
                        Instant.now()),
                claim,
                "SKIPPED");
    }

    private void verificarActualizacion(int updated, Claim claim, String status) {
        if (updated == 0) {
            log.error(
                    "[MarketingDispatch] No se pudo marcar dispatchId={} attempt={} status={}",
                    claim.id(),
                    claim.attempt(),
                    status);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    public record Claim(Long id, UUID idempotencyKey, int attempt) {
    }
}
