package com.yego.backend.scheduler.yego_marketing_mensajes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingDeliveryResult;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingFleetSender;
import com.yego.backend.service.yego_marketing_mensajes.sender.MarketingWhatsAppSender;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMensajeSchedulerTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    @Test
    void enviaCadaCanalSoloCuandoSuBanderaEstaActiva() throws Exception {
        MarketingMensajeRepository repository = mock(MarketingMensajeRepository.class);
        MarketingWhatsAppSender whatsappSender = mock(MarketingWhatsAppSender.class);
        MarketingFleetSender fleetSender = mock(MarketingFleetSender.class);
        Environment environment = mock(Environment.class);
        ObjectMapper objectMapper = new ObjectMapper();

        MarketingMensaje mensaje = mensajeProgramado(objectMapper, true, false);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(repository.findByActivoTrueAndHorasEspecificasIsNotNull())
                .thenReturn(List.of(mensaje));
        when(whatsappSender.enviar(any(), any(), any()))
                .thenReturn(new MarketingDeliveryResult(1, 0, 0, 1));

        new MarketingMensajeScheduler(
                repository, whatsappSender, fleetSender, objectMapper, environment)
                .verificarYEnviarMensajesProgramados();

        verify(whatsappSender).enviar(eq(mensaje), eq(List.of("grupo-1")), any(Instant.class));
        verify(fleetSender, never()).enviar(any(), any(), any());
    }

    private MarketingMensaje mensajeProgramado(
            ObjectMapper objectMapper, boolean whatsapp, boolean fleet) throws Exception {
        LocalDate fecha = LocalDate.now(LIMA);
        String hora = LocalTime.now(LIMA).format(DateTimeFormatter.ofPattern("HH:mm"));

        MarketingMensaje mensaje = new MarketingMensaje();
        mensaje.setId(10L);
        mensaje.setTitulo("Campaña");
        mensaje.setMensaje("Mensaje");
        mensaje.setWhatsapp(whatsapp);
        mensaje.setYandex(fleet);
        mensaje.setGrupos(objectMapper.writeValueAsString(
                whatsapp ? List.of("grupo-1") : List.of()));
        mensaje.setFlotas(objectMapper.writeValueAsString(
                fleet ? List.of("park-1") : List.of()));
        mensaje.setHorasEspecificas(objectMapper.writeValueAsString(
                Map.of(hora, List.of(nombreDia(fecha.getDayOfWeek())))));
        return mensaje;
    }

    @Test
    void enviaWhatsAppCuandoHayGrupoAunqueBanderaSeaFalse() throws Exception {
        MarketingMensajeRepository repository = mock(MarketingMensajeRepository.class);
        MarketingWhatsAppSender whatsappSender = mock(MarketingWhatsAppSender.class);
        MarketingFleetSender fleetSender = mock(MarketingFleetSender.class);
        Environment environment = mock(Environment.class);
        ObjectMapper objectMapper = new ObjectMapper();

        MarketingMensaje mensaje = mensajeProgramado(objectMapper, false, false);
        mensaje.setGrupos(objectMapper.writeValueAsString(List.of("grupo-1")));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(repository.findByActivoTrueAndHorasEspecificasIsNotNull())
                .thenReturn(List.of(mensaje));
        when(whatsappSender.enviar(any(), any(), any()))
                .thenReturn(new MarketingDeliveryResult(1, 0, 0, 1));

        new MarketingMensajeScheduler(
                repository, whatsappSender, fleetSender, objectMapper, environment)
                .verificarYEnviarMensajesProgramados();

        verify(whatsappSender).enviar(eq(mensaje), eq(List.of("grupo-1")), any(Instant.class));
        verify(fleetSender, never()).enviar(any(), any(), any());
    }

    private String nombreDia(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Lun";
            case TUESDAY -> "Mar";
            case WEDNESDAY -> "Mié";
            case THURSDAY -> "Jue";
            case FRIDAY -> "Vie";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }
}
