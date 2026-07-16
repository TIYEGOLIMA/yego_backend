package com.yego.backend.config;

import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.service.yego_principal.ModuleService;
import com.yego.backend.service.yego_principal.WebSocketAccessTicketService;
import com.yego.backend.service.yego_principal.WebSocketModuleMappingService;
import com.yego.backend.service.yego_principal.WebSocketSessionService;
import com.yego.backend.service.yego_principal.WebSocketSessionService.SessionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorScopeTest {

    @Mock
    private ModuleService moduleService;
    @Mock
    private WebSocketSessionService webSocketSessionService;
    @Mock
    private WebSocketModuleMappingService webSocketModuleMappingService;
    @Mock
    private DispositivoRepository dispositivoRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private WebSocketAccessTicketService accessTicketService;
    @Mock
    private MessageChannel channel;
    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @Test
    void tabletPuedeSuscribirseSoloAlRatingDeSuSedeYModulo() {
        registrarTablet(3L, 8L);
        Message<byte[]> message = subscribe("/topic/ticketera/sedes/3/modules/8/rating");

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(webSocketSessionService).addSubscription(
                "tablet-session", "/topic/ticketera/sedes/3/modules/8/rating");
    }

    @Test
    void tabletNoPuedeEscucharElRatingDeOtroModulo() {
        registrarTablet(3L, 8L);
        Message<byte[]> message = subscribe("/topic/ticketera/sedes/3/modules/9/rating");

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNull();
        verify(webSocketSessionService, never()).addSubscription(
                "tablet-session", "/topic/ticketera/sedes/3/modules/9/rating");
    }

    @Test
    void tabletNoPuedeEscucharElRatingDeOtraSede() {
        registrarTablet(3L, 8L);
        Message<byte[]> message = subscribe("/topic/ticketera/sedes/4/modules/8/rating");

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNull();
        verify(webSocketSessionService, never()).addSubscription(
                "tablet-session", "/topic/ticketera/sedes/4/modules/8/rating");
    }

    private void registrarTablet(Long sedeId, Long moduleId) {
        when(webSocketSessionService.isSessionExpired("tablet-session")).thenReturn(false);
        when(webSocketSessionService.getSessionContext("tablet-session")).thenReturn(
                new SessionContext(true, "DEVICE", "TABLET", sedeId, moduleId, Instant.now().plusSeconds(3600)));
    }

    private static Message<byte[]> subscribe(String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("tablet-session");
        accessor.setDestination(destination);
        accessor.setSubscriptionId("subscription-1");
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
