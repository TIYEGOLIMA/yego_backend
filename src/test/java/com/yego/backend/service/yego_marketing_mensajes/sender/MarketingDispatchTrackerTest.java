package com.yego.backend.service.yego_marketing_mensajes.sender;

import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingDispatch;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingDispatchRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingDispatchTrackerTest {

    @Test
    void devuelveClaimSoloCuandoLaEntregaFueAdquirida() {
        MarketingDispatchRepository repository = mock(MarketingDispatchRepository.class);
        MarketingDispatchTracker tracker =
                new MarketingDispatchTracker(repository, 3, 60, 180);
        Instant scheduledFor = Instant.parse("2026-07-13T15:00:00Z");
        MarketingDispatch dispatch = new MarketingDispatch();
        dispatch.setId(5L);
        dispatch.setIdempotencyKey(UUID.randomUUID());
        dispatch.setAttemptCount(2);

        when(repository.claim(
                eq(10L),
                eq(MarketingDispatchTracker.CHANNEL_WHATSAPP),
                eq("grupo-1"),
                eq(scheduledFor),
                any(UUID.class),
                eq(3),
                eq(60),
                eq(180))).thenReturn(1);
        when(repository.findByMessageIdAndChannelAndDestinationIdAndScheduledFor(
                10L,
                MarketingDispatchTracker.CHANNEL_WHATSAPP,
                "grupo-1",
                scheduledFor)).thenReturn(Optional.of(dispatch));

        Optional<MarketingDispatchTracker.Claim> claim = tracker.reclamar(
                10L,
                MarketingDispatchTracker.CHANNEL_WHATSAPP,
                "grupo-1",
                scheduledFor);

        assertThat(claim).contains(new MarketingDispatchTracker.Claim(
                5L, dispatch.getIdempotencyKey(), 2));
    }

    @Test
    void noReenviaCuandoLaEntregaNoPuedeReclamarse() {
        MarketingDispatchRepository repository = mock(MarketingDispatchRepository.class);
        MarketingDispatchTracker tracker =
                new MarketingDispatchTracker(repository, 3, 60, 180);
        Instant scheduledFor = Instant.parse("2026-07-13T15:00:00Z");
        when(repository.claim(
                eq(10L),
                eq(MarketingDispatchTracker.CHANNEL_FLEET),
                eq("park-1"),
                eq(scheduledFor),
                any(UUID.class),
                eq(3),
                eq(60),
                eq(180))).thenReturn(0);

        assertThat(tracker.reclamar(
                10L,
                MarketingDispatchTracker.CHANNEL_FLEET,
                "park-1",
                scheduledFor)).isEmpty();
        verify(repository).claim(
                eq(10L),
                eq(MarketingDispatchTracker.CHANNEL_FLEET),
                eq("park-1"),
                eq(scheduledFor),
                any(UUID.class),
                eq(3),
                eq(60),
                eq(180));
    }
}
